package com.mybatis.mybatis.plugin.process;

import com.mybatis.mybatis.plugin.config.PluginConfig;
import com.mybatis.mybatis.plugin.config.PluginLevelType;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @program: mybatis plugin
 * @description: 插件级别的过滤，校验是否符合规则
 * @author: lengrongfu
 * @created: 2020/08/15 00:11
 */
public class PluginLevelValidate {

    /**
     * @Description: 校验 level 对应的值是否符合当前sql
     * @return:
     * @Creator: lengrongfu
     * @Date: 2020/8/15 12:15 上午
     */
    public static PluginLevelValidate DEFAULT = new PluginLevelValidate();


    public Boolean validateLevel(PluginConfig plugin, Statement statement) {
        PluginLevelType level = plugin.getLevel();
        List<String> values = plugin.getValue();
        List<String> ignoreTables = plugin.getIgnoreTables();
        if (CollectionUtils.isEmpty(values)) {
            values = new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(ignoreTables)) {
            ignoreTables = new ArrayList<>();
        }
        List<String> lowerCaseValues = values.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<String> lowerCaseIgnoreTables = ignoreTables.stream().map(String::toLowerCase).collect(Collectors.toList());
        if (PluginLevelType.dml.equals(level)) {
            if (statement instanceof Insert
                    && !lowerCaseIgnoreTables.contains(SqlCommandType.INSERT.name().toLowerCase())
                    && lowerCaseValues.contains(SqlCommandType.INSERT.name().toLowerCase())) {
                return true;
            } else if (statement instanceof Select
                    && !lowerCaseIgnoreTables.contains(SqlCommandType.SELECT.name().toLowerCase())
                    && lowerCaseValues.contains(SqlCommandType.SELECT.name().toLowerCase())) {
                return true;
            } else if (statement instanceof Update
                    && !lowerCaseIgnoreTables.contains(SqlCommandType.UPDATE.name().toLowerCase())
                    && lowerCaseValues.contains(SqlCommandType.UPDATE.name().toLowerCase())) {
                return true;
            } else if (statement instanceof Delete
                    && !lowerCaseIgnoreTables.contains(SqlCommandType.DELETE.name().toLowerCase())
                    && lowerCaseValues.contains(SqlCommandType.DELETE.name().toLowerCase())) {
                return true;
            }
        } else if (PluginLevelType.table.equals(level)) {
            Table table = statementInstanceof(statement);
            if (Objects.isNull(table)) {
                return false;
            }
            String tableName = table.getName();
            if (lowerCaseIgnoreTables.contains(tableName.toLowerCase())) {
                //在忽略表集合中，则不再自动添加租户id
                return false;
            }
            if (lowerCaseValues.contains("all")) {
                return true;
            }
            if (lowerCaseValues.contains(tableName.toLowerCase())) {
                return true;
            }
            for (String name :lowerCaseValues ) {
                if(name.contains("**")){
                    // 模糊配置
                  name =  name.replaceAll("\\*\\*", "");
                 if(tableName.toLowerCase().contains(name) ){
                     return  true;
                 }
                }
            }
        } else if (PluginLevelType.databases.equals(level)) {
            Table table = statementInstanceof(statement);
            if (Objects.isNull(table)) {
                return false;
            }
            String databaseName = table.getSchemaName();
            if (Objects.nonNull(databaseName) &&
                    lowerCaseValues.contains(databaseName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private Table statementInstanceof(Statement statement) {
        Table table = null;
        if (statement instanceof Insert) {
            Insert insert = (Insert) statement;
            table = insert.getTable();

        } else if (statement instanceof Select) {
            SelectBody select = ((Select) statement).getSelectBody();
            table = processSelectBody(select);
        } else if (statement instanceof Update) {
            Update update = (Update) statement;
          //  table = update.getTables().get(0);
            table = update.getTable();
        } else if (statement instanceof Delete) {
            Delete delete = (Delete) statement;
            table = delete.getTable();
        }
        return table;
    }

    private Table processSelectBody(SelectBody selectBody) {
        if (selectBody instanceof PlainSelect) {
            FromItem fromItem = ((PlainSelect) selectBody).getFromItem();
            if (fromItem instanceof Table) {
                return (Table) fromItem;
            }
            //解决子查询无法解析问题：如果是子查询，则递归解析子查询
            if(fromItem instanceof SubSelect) {
                return processSelectBody(((SubSelect)fromItem).getSelectBody());
            }

            return  processSelectBody(((SubSelect) fromItem).getSelectBody());

        }
        //解决union all无法解析问题：如果是union all 解析其中子查询
        if(selectBody instanceof SetOperationList) {
            List<SelectBody> selects = ((SetOperationList) selectBody).getSelects();
            if(selects != null && !selects.isEmpty()) {
                for(int i = 0; i < selects.size(); i++) {
                    Table table =  processSelectBody(selects.get(i));
                    if(table != null) {
                        return table;
                    }
                }
            }
        }
//        else if (selectBody instanceof WithItem) {
//            WithItem withItem = (WithItem) selectBody;
//            if (withItem.getSelectBody() != null) {
//                processSelectBody(withItem.getSelectBody());
//            }
//        } else if (selectBody instanceof SetOperationList) {
//            SetOperationList operationList = (SetOperationList) selectBody;
//            if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {
//                operationList.getSelects().forEach(this::processSelectBody);
//            }
//        } else if (selectBody instanceof ValuesStatement) {
//            ValuesStatement valuesStatement = (ValuesStatement) selectBody;
//            List<Expression> expressions = valuesStatement.getExpressions();
//            for (Expression expression : expressions) {
//            }
//        }
        return null;
    }
}
