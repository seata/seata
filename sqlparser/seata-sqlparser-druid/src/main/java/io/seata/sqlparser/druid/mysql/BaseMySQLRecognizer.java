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
package io.seata.sqlparser.druid.mysql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;

/**
 * @author will
 */
public abstract class BaseMySQLRecognizer extends BaseRecognizer {

    /**
     * Instantiates a new mysql base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseMySQLRecognizer(String originalSql) {
        super(originalSql);
    }

    public MySqlOutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
                                                  final ArrayList<List<Object>> paramAppenderList,
                                                  final StringBuilder sb) {
        return new MySqlOutputVisitor(sb) {

            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
                        oneParamValues.forEach(t -> paramAppenderList.add(new ArrayList<>()));
                    }
                    for (int i = 0; i < oneParamValues.size(); i++) {
                        Object o = oneParamValues.get(i);
                        paramAppenderList.get(i).add(o instanceof Null ? null : o);
                    }
                }
                return super.visit(x);
            }
        };
    }

    public String getWhereCondition(SQLExpr where, final ParametersHolder parametersHolder,
                                    final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeVisit(where, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    public String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeVisit(where, new MySqlOutputVisitor(sb));
        return sb.toString();
    }

    protected String getLimitCondition(SQLLimit sqlLimit) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeLimit(sqlLimit, new MySqlOutputVisitor(sb));

        return sb.toString();
    }

    protected String getLimitCondition(SQLLimit sqlLimit, final ParametersHolder parametersHolder,
                                       final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeLimit(sqlLimit, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, new MySqlOutputVisitor(sb));

        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy, final ParametersHolder parametersHolder,
                                         final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

}
