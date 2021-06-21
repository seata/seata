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
package io.seata.core.store.querier;

import io.seata.common.util.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;

/**
 * @author wang.liang
 */
public interface Sortable {

    /**
     * Gets sort params.
     *
     * @return the sort params
     */
    SortParam[] getSortParams();

    /**
     * Sets sort params.
     *
     * @param sortParams the sort params
     */
    void setSortParams(SortParam... sortParams);

    /**
     * Has sort params.
     *
     * @return the boolean
     */
    default boolean hasSortParams() {
        return ArrayUtils.isNotEmpty(getSortParams());
    }

    /**
     * Is need sort.
     *
     * @return the boolean
     */
    default boolean isNeedSort(List<?> dataList) {
        return CollectionUtils.isNotEmpty(dataList) && this.hasSortParams();
    }
}
