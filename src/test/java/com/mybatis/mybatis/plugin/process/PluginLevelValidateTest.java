package com.mybatis.mybatis.plugin.process;

import com.mybatis.mybatis.plugin.config.PluginConfig;
import com.mybatis.mybatis.plugin.config.PluginLevelType;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
public class PluginLevelValidateTest {

    @InjectMocks
    private PluginLevelValidate pluginLevelValidate;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void validateDMLLevel() {
        PluginConfig plugin = new PluginConfig();
        plugin.setLevel(PluginLevelType.dml);
        plugin.setName("select_dml");
        plugin.setValue(Arrays.asList("select"));
        Statement statement = new Select();
        Boolean validateLevel = pluginLevelValidate.validateLevel(plugin, statement);
        assert validateLevel;


        plugin.setValue(Arrays.asList("INSERT"));
        statement = new Insert();
        validateLevel = pluginLevelValidate.validateLevel(plugin, statement);
        assert validateLevel;

        plugin.setValue(Arrays.asList("update"));
        statement = new Update();
        validateLevel = pluginLevelValidate.validateLevel(plugin, statement);
        assert validateLevel;

        plugin.setValue(Arrays.asList("DELETE"));
        statement = new Delete();
        validateLevel = pluginLevelValidate.validateLevel(plugin, statement);
        assert validateLevel;

    }

    @Test
    public void validateTableLevel() throws JSQLParserException {
        PluginConfig plugin = new PluginConfig();
        plugin.setLevel(PluginLevelType.table);
        plugin.setName("user_table");
        plugin.setValue(Arrays.asList("user"));


        /**
         * table is null
         */
        Insert insert = new Insert();
        Boolean validateLevel = pluginLevelValidate.validateLevel(plugin, insert);
        assert !validateLevel;

        /**
         * insert table
         */
        insert = (Insert) CCJSqlParserUtil.parse("insert into user(name) value('zs')");
        validateLevel = pluginLevelValidate.validateLevel(plugin, insert);
        assert validateLevel;

        /**
         * update table
         */
        Update update = (Update) CCJSqlParserUtil.parse("update user set name='ls' where id=1");
        validateLevel = pluginLevelValidate.validateLevel(plugin, update);
        assert validateLevel;

        /**
         * delete table
         */
        Delete delete = (Delete) CCJSqlParserUtil.parse("delete from user where id = 1");
        validateLevel = pluginLevelValidate.validateLevel(plugin, delete);
        assert validateLevel;

        /**
         * select PlainSelect table
         */
        Select select = (Select) CCJSqlParserUtil.parse("select * from user");
        validateLevel = pluginLevelValidate.validateLevel(plugin, select);
        assert validateLevel;

        /**
         * table is *
         */
        plugin.setValue(Arrays.asList("all"));
        Select select1 = (Select) CCJSqlParserUtil.parse("select * from user1");
        validateLevel = pluginLevelValidate.validateLevel(plugin, select1);
        assert validateLevel;

        Select select2 = (Select) CCJSqlParserUtil.parse("select * from user2");
        validateLevel = pluginLevelValidate.validateLevel(plugin, select2);
        assert validateLevel;

        Select select3 = (Select) CCJSqlParserUtil.parse("select * from user3");
        validateLevel = pluginLevelValidate.validateLevel(plugin, select3);
        assert validateLevel;
    }


    @Test
    public void validateDatabaseLevel() throws JSQLParserException {

        PluginConfig plugin = new PluginConfig();
        plugin.setLevel(PluginLevelType.databases);
        plugin.setName("test_database");
        plugin.setValue(Arrays.asList("test"));


        Insert insert = (Insert) CCJSqlParserUtil.parse("insert into test.user(name) value('zs')");
        Boolean validateLevel = pluginLevelValidate.validateLevel(plugin, insert);
        assert validateLevel;


        plugin.setValue(Arrays.asList("test_1"));
        validateLevel = pluginLevelValidate.validateLevel(plugin, insert);
        assert !validateLevel;

    }

    @Test
    public void validateSelect() throws JSQLParserException {
        PluginConfig plugin = new PluginConfig();
        plugin.setLevel(PluginLevelType.table);
//        plugin.setName("user_table");
        plugin.setValue(Arrays.asList("wms_stock_out_task"));
        Select select = (Select) CCJSqlParserUtil.parse("SELECT DISTINCT r.role_id, r.role_name, r.role_key, r.role_sort, r.data_scope, r.menu_check_strictly, r.dept_check_strictly, r.status, r.del_flag, r.create_time, r.remark FROM sys_role r LEFT JOIN sys_user_role ur ON ur.role_id = r.role_id LEFT JOIN sys_user u ON u.user_id = ur.user_id LEFT JOIN sys_dept d ON u.dept_id = d.dept_id WHERE r.del_flag = '0'");
        Select select2 = (Select) CCJSqlParserUtil.parse("SELECT count(0) FROM (SELECT DISTINCT r.role_id, r.role_name, r.role_key, r.role_sort, r.data_scope, r.menu_check_strictly, r.dept_check_strictly, r.status, r.del_flag, r.create_time, r.remark FROM sys_role r LEFT JOIN sys_user_role ur ON ur.role_id = r.role_id LEFT JOIN sys_user u ON u.user_id = ur.user_id LEFT JOIN sys_dept d ON u.dept_id = d.dept_id WHERE r.del_flag = '0') table_count");
        Select select3 = (Select) CCJSqlParserUtil.parse("(SELECT r.role_id,r.role_name,r.role_key,r.role_sort,r.data_scope,r.menu_check_strictly,r.dept_check_strictly,r.STATUS,r.del_flag,r.create_time,r.remark FROM sys_role r WHERE role_id IN (1,2)) UNION ALL (SELECT r.role_id,r.role_name,r.role_key,r.role_sort,r.data_scope,r.menu_check_strictly,r.dept_check_strictly,r.STATUS,r.del_flag,r.create_time,r.remark FROM sys_role r WHERE role_id IN (100,103))");

        String sql = "SELECT\n" +
                "\tsot.production_order_delivery_no,\n" +
                "\tsot.task_type,\n" +
                "\tsot.active_num,\n" +
                "\tCONCAT(\n" +
                "\t\tsot.production_order_delivery_no,\n" +
                "\t\t'-',\n" +
                "\tIFNULL( sot.active_num, 1 )) multiple_active_no,\n" +
                "\tsot.is_printed,\n" +
                "\tpod.plan_commencement_date,\n" +
                "\tpodd.production_order_no,\n" +
                "\tsot.location_code,\n" +
                "\tpodd.work_shop \n" +
                "FROM\n" +
                "\twms_stock_out_task sot\n" +
                "\tLEFT JOIN wms_production_order_detail podd ON podd.id = sot.production_order_detail_id\n" +
                "\tLEFT JOIN wms_production_order_deliver pod ON pod.id = sot.production_order_delivery_id \n" +
                "WHERE\n" +
                "\ttask_type = 1 \n" +
                "\tAND task_status != 4 \n" +
                "GROUP BY\n" +
                "\tproduction_order_delivery_no,\n" +
                "\tactive_num,\n" +
                "\tis_printed,\n" +
                "\ttask_type,\n" +
                "\tpodd.production_order_no,\n" +
                "\tlocation_code,\n" +
                "\tpodd.work_shop,\n" +
                "\tpod.plan_commencement_date UNION ALL\n" +
                "SELECT\n" +
                "\tsot.production_order_delivery_no,\n" +
                "\tsot.task_type,\n" +
                "\tsot.active_num,\n" +
                "\tCONCAT(\n" +
                "\t\tsot.production_order_delivery_no,\n" +
                "\t\t'-',\n" +
                "\tIFNULL( sot.active_num, 1 )) multiple_active_no,\n" +
                "\tsot.is_printed,\n" +
                "\tpro.plan_commencement_date,\n" +
                "\tpro.production_order_no,\n" +
                "\tsot.location_code,\n" +
                "\tprod.work_shop \n" +
                "FROM\n" +
                "\twms_stock_out_task sot\n" +
                "\tLEFT JOIN wms_production_replenishment_order_detail prod ON prod.id = sot.production_order_detail_id\n" +
                "\tLEFT JOIN wms_production_replenishment_order pro ON prod.replenishment_order_no = pro.order_no \n" +
                "WHERE\n" +
                "\ttask_type = 2 \n" +
                "\tAND task_status != 4 \n" +
                "GROUP BY\n" +
                "\tproduction_order_delivery_no,\n" +
                "\tactive_num,\n" +
                "\tis_printed,\n" +
                "\ttask_type,\n" +
                "\tlocation_code,\n" +
                "\tpro.production_order_no,\n" +
                "\tprod.work_shop,\n" +
                "\tpro.plan_commencement_date";

        Select select4 = (Select) CCJSqlParserUtil.parse(sql);

        Boolean validateLevel = pluginLevelValidate.validateLevel(plugin, select4);
        assert validateLevel;
    }
}