/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.sqlparser.antlr.mysql;

import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.antlr.mysql.listener.UpdateSpecificationSqlListener;
import io.seata.sqlparser.antlr.mysql.parser.MySqlLexer;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.stream.ANTLRNoCaseStringStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AntlrMySQLUpdateRecognizer
 *
 * @author zhihou
 */
public class AntlrMySQLUpdateRecognizer implements SQLUpdateRecognizer {

    private MySqlContext sqlContext;

    public AntlrMySQLUpdateRecognizer(String sql) {
        MySqlLexer mySqlLexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream commonTokenStream = new CommonTokenStream(mySqlLexer);
        MySqlParser parser2 = new MySqlParser(commonTokenStream);
        MySqlParser.RootContext root = parser2.root();
        ParseTreeWalker walker2 = new ParseTreeWalker();
        sqlContext = new MySqlContext();
        sqlContext.setOriginalSQL(sql);
        walker2.walk(new UpdateSpecificationSqlListener(sqlContext), root);
    }

    @Override
    public List<String> getUpdateColumns() {

        List<MySqlContext.SQL> updateFoColumnNames = sqlContext.getUpdateFoColumnNames();
        List<String> sqlList = new ArrayList<>();
        for (MySqlContext.SQL sql : updateFoColumnNames) {
            sqlList.add(sql.getUpdateColumn());
        }
        return sqlList;
    }

    @Override
    public List<Object> getUpdateValues() {

        List<MySqlContext.SQL> updateForValues = sqlContext.getUpdateForValues();

        if (updateForValues.isEmpty()) {
            return new ArrayList<>();
        }

        return updateForValues.stream().map(updateValues -> updateValues.getUpdateValue()).collect(Collectors.toList());
    }

    @Override
    public String getWhereCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return sqlContext.getWhereCondition();
    }

    @Override
    public String getWhereCondition() {
        return sqlContext.getWhereCondition();
    }

    @Override
    public String getLimitCondition() {
        return null;
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    public String getOrderByCondition() {
        return null;
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.UPDATE;
    }

    @Override
    public String getTableAlias() {
        return sqlContext.tableAlias;
    }

    @Override
    public String getTableName() {
        return sqlContext.tableName;
    }

    @Override
    public String getOriginalSQL() {
        return sqlContext.getOriginalSQL();
    }
}