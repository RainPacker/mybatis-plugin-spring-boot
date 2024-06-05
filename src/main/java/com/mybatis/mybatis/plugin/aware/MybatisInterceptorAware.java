package com.mybatis.mybatis.plugin.aware;

import com.mybatis.mybatis.plugin.Filtered;
import com.mybatis.mybatis.plugin.IgnoreTenant;
import com.mybatis.mybatis.plugin.process.PluginsProcess;
import com.mybatis.mybatis.plugin.utils.SqlParserUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @program: mybatis plugin
 * @description:
 * @author: lengrongfu
 * @created: 2020/08/14 23:05
 */
public class MybatisInterceptorAware implements InterceptorAware, InterceptorAwareRegister {

    private static final Logger logger = LoggerFactory.getLogger(MybatisInterceptorAware.class);

    private PluginsProcess pluginsProcess;

    public MybatisInterceptorAware(PluginsProcess pluginsProcess) {
        this.pluginsProcess = pluginsProcess;
    }

    final String PAGE_HELP_COUNT = "_COUNT";

    @Override
    public void mybatisBeforeExecutor(Invocation invocation) {
        if (invocation.getTarget() instanceof Executor) {
            Object[] args = invocation.getArgs();
            MappedStatement statement = (MappedStatement) args[0];
            BoundSql boundSql;
            if (args.length == 4 || args.length == 2) {
                boundSql = statement.getBoundSql(args[1]);
            } else {
                // 几乎不可能走进这里面,除非使用Executor的代理对象调用query[args[6]]
                boundSql = (BoundSql) args[5];
            }

            //获取类
            String namespace = statement.getId();
            String className = namespace.substring(0,namespace.lastIndexOf("."));
            String methedName= namespace.substring(namespace.lastIndexOf(".") + 1,namespace.length());
            Method[] ms = new Method[0];
            try {
                Class cls = Class.forName(className);

                Annotation annotation = cls.getAnnotation(IgnoreTenant.class);
                if (annotation != null){
                    //如果Mapper类中包含IgnoreTenant注解，则不需要自动添加租户过滤条件
                    return;
                }
                ms = cls.getMethods();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Executor executor = (Executor)invocation.getTarget();
            if(methedName != null && methedName.endsWith(PAGE_HELP_COUNT)) {
                //解决分页插件中count与真实查询SQL可能不匹配的问题
                methedName = methedName.substring(0, methedName.length() - PAGE_HELP_COUNT.length());
            }
            for(Method m : ms){
                if(m.getName().equals(methedName)){
                    Annotation annotation = m.getAnnotation(IgnoreTenant.class);
                    if (annotation != null){
                        //如果方法中包含IgnoreTenant注解，则不需要自动添加租户过滤条件
                        return;
                    }
                }
            }

            String sql = boundSql.getSql();
            // 解析SQL判断where条件中是否包含tenant_id 或 insert字段中包含了tenant_id
            if(SqlParserUtil.whereHasTenantId(sql) || SqlParserUtil.insertHasTenantId(sql)) {
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("mybatisBeforeExecutor old sql {}", sql);
            }

            String newSql = pluginsProcess.pluginsProcess(sql);

            if (logger.isDebugEnabled()) {
                logger.debug("mybatisBeforeExecutor new sql {}", sql);
            }
            
            MappedStatement newStatement = newMappedStatement(statement, new BoundSqlSqlSource(boundSql));
            MetaObject msObject = MetaObject.forObject(newStatement, new DefaultObjectFactory(),
                    new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());
            msObject.setValue("sqlSource.boundSql.sql", newSql);
            args[0] = newStatement;
        }
    }

    @Override
    public void mybatisAfterExecutor(Object result) {

    }

    @Override
    public void registerInterceptorAware(InterceptorAwareCollect interceptorAwareCollect) {
        interceptorAwareCollect.addInterceptorAware(this);
    }

    private MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder =
                new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    //    定义一个内部辅助类，作用是包装sq
    class BoundSqlSqlSource implements SqlSource {
        private BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

}
