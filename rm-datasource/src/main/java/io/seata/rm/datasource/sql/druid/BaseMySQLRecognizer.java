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
package io.seata.rm.datasource.sql.druid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;

import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.ParametersHolder;

/**
 * @author will
 * @date 2019/9/26
 */
public abstract class BaseMySQLRecognizer extends BaseRecognizer {

    /**
     * Instantiates a new mysql base recognizer
     * @param originalSql the original sql
     */
    public BaseMySQLRecognizer(String originalSql){
        super(originalSql);
    }

    public MySqlOutputVisitor createOutputVisitor(final ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList, final StringBuilder sb) {
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(sb) {

            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters()[x.getIndex()];
                    if (paramAppenderList.size() == 0) {
                        oneParamValues.stream().forEach(t -> paramAppenderList.add(new ArrayList<>()));
                    }
                    for (int i = 0; i < oneParamValues.size(); i++) {
                        paramAppenderList.get(i).add(oneParamValues.get(i));
                    }

                }
                return super.visit(x);
            }
        };
        return visitor;
    }

    public String getWhereCondition(SQLExpr where, final ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        excuteVisit(where, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    public String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        excuteVisit(where, new MySqlOutputVisitor(sb));
        return sb.toString();
    }

    private void excuteVisit(SQLExpr where, SQLASTVisitor visitor) {
        if (where instanceof SQLBinaryOpExpr) {
            visitor.visit((SQLBinaryOpExpr) where);
        } else if (where instanceof SQLInListExpr) {
            visitor.visit((SQLInListExpr) where);
        } else if (where instanceof SQLBetweenExpr) {
            visitor.visit((SQLBetweenExpr) where);
        } else if (where instanceof SQLExistsExpr) {
            visitor.visit((SQLExistsExpr) where);
        } else {
            throw new IllegalArgumentException("unexpected WHERE expr: " + where.getClass().getSimpleName());
        }
    }

}
