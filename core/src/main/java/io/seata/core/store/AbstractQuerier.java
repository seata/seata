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
package io.seata.core.store;

import io.seata.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static io.seata.common.DefaultValues.FIRST_PAGE_INDEX;

/**
 * @author wang.liang
 */
public abstract class AbstractQuerier<T> implements Querier<T>, Sortable, Pageable {

    // sort params
    protected SortParam[] sortParams;

    // page params
    protected int pageIndex = FIRST_PAGE_INDEX;
    protected int pageSize = 0;

    /**
     * Do paging.
     *
     * @param list the list
     * @return the list after paging
     */
    @Override
    public <D extends T> List<D> doPaging(List<D> list) {
        if (list == null) {
            return new ArrayList<>();
        }

        if (list.isEmpty() || getPageSize() <= 0) {
            return list;
        }

        int fromIndex = this.getFromIndex();
        int toIndex = this.getToIndex(fromIndex);

        if (fromIndex >= list.size()) {
            return new ArrayList<>();
        }

        if (toIndex > list.size()) {
            toIndex = list.size();
        }

        return list.subList(fromIndex, toIndex);
    }

    public int getFromIndex() {
        return (pageIndex - FIRST_PAGE_INDEX) * pageSize;
    }

    public int getToIndex(int fromIndex) {
        return fromIndex + pageSize;
    }

    /**
     * do sort
     *
     * @param globalTransactionDOs the global transactions
     * @return the after sort list
     */
    @Override
    public <D extends T> List<D> doSort(List<D> globalTransactionDOs) {
        if (CollectionUtils.isEmpty(globalTransactionDOs)) {
            return new ArrayList<>();
        }

        if (!this.isNeedSort(globalTransactionDOs)) {
            return globalTransactionDOs;
        }

        globalTransactionDOs.sort((a, b) -> {
            int ret;
            for (SortParam sortParam : this.getSortParams()) {
                ret = this.compareByFieldName(a, b, sortParam.getSortFieldName());
                if (ret == 0) {
                    continue;
                }

                if (sortParam.getSortOrder() == SortOrder.DESC) {
                    ret = ret > 0 ? -1 : 1; // -1 * ret
                }
                return ret;
            }
            return 0;
        });
        return globalTransactionDOs;
    }

    /**
     * Compare by field name.
     *
     * @param a             the object a
     * @param b             the object b
     * @param sortFieldName the sort field name
     * @return the compare result
     */
    public abstract <D extends T> int compareByFieldName(D a, D b, String sortFieldName);

    @Override
    public SortParam[] getSortParams() {
        return sortParams;
    }

    @Override
    public void setSortParams(SortParam... sortParams) {
        this.sortParams = sortParams;
    }

    @Override
    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
