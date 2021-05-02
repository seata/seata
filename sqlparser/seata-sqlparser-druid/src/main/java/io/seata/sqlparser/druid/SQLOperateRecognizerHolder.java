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
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.SQLRecognizer;

/**
 * The interface SQLOperateRecognizerHolder
 *
 * @author: Zhibei Hao
 */
public interface SQLOperateRecognizerHolder {

    /**
     * Get delete recognizer
     *
     * @param sql the sql
     * @param ast the ast
     * @return the delete recognizer
     */
    SQLRecognizer getDeleteRecognizer(String sql, SQLStatement ast);

    /**
     * Get insert recognizer
     *
     * @param sql the sql
     * @param ast the ast
     * @return the insert recognizer
     */
    SQLRecognizer getInsertRecognizer(String sql, SQLStatement ast);

    /**
     * Get update recognizer
     *
     * @param sql the sql
     * @param ast the ast
     * @return the update recognizer
     */
    SQLRecognizer getUpdateRecognizer(String sql, SQLStatement ast);

    /**
     * Get SelectForUpdate recognizer
     *
     * @param sql the sql
     * @param ast the ast
     * @return the SelectForUpdate recognizer
     */
    SQLRecognizer getSelectForUpdateRecognizer(String sql, SQLStatement ast);

    /**
     * Get select recognizer
     *
     * @param sql the sql
     * @param ast the ast
     * @return the SelectForUpdate recognizer
     */
    SQLRecognizer getSelectRecognizer(String sql, SQLStatement ast);
}
