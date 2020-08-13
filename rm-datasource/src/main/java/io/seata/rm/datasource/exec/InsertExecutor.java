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
package io.seata.rm.datasource.exec;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author jsbxyyx
 */
public interface InsertExecutor<T> extends Executor<T> {

    /**
     * get primary key values.
     *
     * @return The primary key value.
     * @throws SQLException
     */
    Map<String, List<Object>> getPkValues() throws SQLException;

    /**
     * get primary key values by insert column.
     *
     * @return
     * @throws SQLException
     */
    Map<String, List<Object>> getPkValuesByColumn() throws SQLException;

}
