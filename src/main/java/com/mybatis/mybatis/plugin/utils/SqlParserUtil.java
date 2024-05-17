package com.mybatis.mybatis.plugin.utils;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.Collection;
import java.util.List;

/**
 * @author shizhiqiang
 * @description: 解析SQL, 判断SQL语句的条件语句（where 或 on) 以及子查询中是否包含租户字段。
 * @date 2024/5/16 11:01
 */
public class SqlParserUtil {

    private final static String TENANT_ID = "tenant_id";

    /**
     * @description: 判断包含 tenant_id 的SQL中是否出现在 where 条件中
     * @author shizhiqiang
     * @date: 2024/5/16 10:45
     * @param sql 带解析的SQL
     * @return boolean  where条件中包含 tenant_id 则返回true, 否则返回false
     */
    public static boolean whereHasTenantId(String sql) {
        if(sql == null || !sql.toString().toLowerCase().contains(TENANT_ID)) {
            return false;
        }
        Statement statement = null;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            return false;
        }
        return whereHasTenantId(statement);
    }

    /**
     * @description: 解析 Statement 对象
     * @author shizhiqiang
     * @date: 2024/5/16 14:35
     * @param statement
     * @return boolean
     */
    public static boolean whereHasTenantId(Statement statement) {
        if(statement == null || !statement.toString().toLowerCase().contains(TENANT_ID)) {
            return false;
        }
        if(statement instanceof Select) {
            return whereHasTenantId(((Select)statement).getSelectBody());
        }
        return false;
    }

    /**
     * @description: 解析 SelectBody
     * @author shizhiqiang
     * @date: 2024/5/16 14:34
     * @param selectBody
     * @return boolean
     */
    public static boolean whereHasTenantId(SelectBody selectBody) {
        if(selectBody == null || !selectBody.toString().toLowerCase().contains(TENANT_ID)) {
            return false;
        }
        if(selectBody instanceof PlainSelect) {
            Expression where = ((PlainSelect) selectBody).getWhere();
            if(whereHasTenantId(where)) {
                return true;
            }
            List<Join> joins = ((PlainSelect) selectBody).getJoins();
            if(joins != null) {
                for (int i = 0; i < joins.size(); i++) {
                    if(whereHasTenantId(joins.get(i))) {
                        return true;
                    }
                }
            }
            // 子查询情况
            FromItem fromItem = ((PlainSelect) selectBody).getFromItem();
            if(whereHasTenantId(fromItem)) {
                return true;
            }
        }
        // union all 情况
        if(selectBody instanceof SetOperationList) {
            List<SelectBody> selects = ((SetOperationList) selectBody).getSelects();
            if(selects == null || selects.isEmpty()) {
                return false;
            }
            for (SelectBody select : selects) {
                if(whereHasTenantId(select)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @description: 解析 FromItem 可能的子查询情况
     * @author shizhiqiang
     * @date: 2024/5/16 14:57
     * @param fromItem
     * @return boolean
     */
    public static boolean whereHasTenantId(FromItem fromItem) {
        if(fromItem == null || !fromItem.toString().toLowerCase().contains(TENANT_ID)) {
            return false;
        }
        if(fromItem instanceof SubSelect) {
            return whereHasTenantId(((SubSelect) fromItem).getSelectBody());
        }
        return false;
    }

    /**
     * @description: 解析 Expression 中是否包含租户条件
     * @author shizhiqiang
     * @date: 2024/5/16 13:42
     * @param expression
     * @return boolean
     */
    public static boolean whereHasTenantId(Expression expression) {
        if(expression == null || !expression.toString().toLowerCase().contains(TENANT_ID)) {
            return false;
        }
        // and or = 等二元条件表达式
        if(expression instanceof AndExpression
                || expression instanceof OrExpression
                || expression instanceof EqualsTo
                || expression instanceof BinaryExpression) {
            Expression leftExpression = ((BinaryExpression) expression).getLeftExpression();
            Expression rightExpression = ((BinaryExpression) expression).getRightExpression();
            return whereHasTenantId(leftExpression) || whereHasTenantId(rightExpression);
        }

        // in 条件表达式
        if(expression instanceof InExpression) {
            Expression leftExpression = ((InExpression) expression).getLeftExpression();
            ItemsList itemsList = ((InExpression) expression).getRightItemsList();

            return whereHasTenantId(leftExpression) || whereHasTenantId(itemsList);
        }

        if(expression instanceof Function) {
            ExpressionList parameters = ((Function) expression).getParameters();
            return whereHasTenantId(parameters);
        }

        if(expression instanceof Between) {
            Expression leftExpression = ((Between) expression).getLeftExpression();
            if(whereHasTenantId(leftExpression)) {
                return true;
            }
            Expression betweenExpressionStart = ((Between) expression).getBetweenExpressionStart();
            Expression betweenExpressionEnd = ((Between) expression).getBetweenExpressionEnd();
            return whereHasTenantId(betweenExpressionStart) || whereHasTenantId(betweenExpressionEnd);
        }
        // 表达式是子查询的情况
        if(expression instanceof SubSelect) {
            return whereHasTenantId(((SubSelect)expression).getSelectBody());
        }

        // 字段表达式
        if(expression instanceof Column) {
            return TENANT_ID.equalsIgnoreCase(((Column) expression).getColumnName());
        }

        return false;
    }

    /**
     * @description: 解析 ItemsList
     * @author shizhiqiang
     * @date: 2024/5/16 13:54
     * @param itemsList
     * @return boolean
     */
    public static boolean whereHasTenantId(ItemsList itemsList) {
        if(itemsList == null || !itemsList.toString().toLowerCase().contains(TENANT_ID)) {
            return false;
        }
        if(itemsList instanceof SubSelect) {
            return whereHasTenantId(((SubSelect) itemsList).getSelectBody());
        }
        if(itemsList instanceof ExpressionList) {
            List<Expression> expressions = ((ExpressionList) itemsList).getExpressions();
            for(int i = 0; i < expressions.size(); i++) {
                if(whereHasTenantId(expressions.get(i))) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * @description: 解析 join 子句
     * @author shizhiqiang
     * @date: 2024/5/16 14:29
     * @param join
     * @return boolean
     */
    public static boolean whereHasTenantId(Join join) {
        if(join == null || !join.toString().toLowerCase().contains(TENANT_ID)) {
            return false;
        }
        Collection<Expression> onExpressions = join.getOnExpressions();
        if(onExpressions == null || onExpressions.isEmpty()) {
            return false;
        }
        for (Expression onExpression : onExpressions) {
            if(whereHasTenantId(onExpression)) {
                return true;
            }
        }

        return false;
    }

}
