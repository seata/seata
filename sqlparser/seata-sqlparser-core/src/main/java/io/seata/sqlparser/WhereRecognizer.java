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
package io.seata.sqlparser;

import java.util.ArrayList;
import java.util.List;

/**
 * The interface Where recognizer.
 *
 * @author sharajava
 */
public interface WhereRecognizer extends SQLRecognizer {

    /**
     * Gets where condition.
     *
     * @param parametersHolder the parameters holder
     * @param paramAppenderList    the param appender list
     * @return the where condition
     */
    String getWhereCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList);

    /**
     * Gets where condition.
     *
     * @return the where condition
     */
    String getWhereCondition();

    /**
     * Gets limit condition
     *
     * @return the limit condition
     */
    String getLimitCondition();

    /**
     * Gets limit condition
     * @param parametersHolder
     * @param paramAppenderList
     * @return the limit condition
     */
    String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList);

    /**
     * Gets order by condition.
     *
     * @return the order by condition
     */
    String getOrderByCondition();

    /**
     * Gets order by condition.
     * @param parametersHolder
     * @param paramAppenderList
     * @return the order by condition
     */
    String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList);

}
