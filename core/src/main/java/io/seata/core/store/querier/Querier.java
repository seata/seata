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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.seata.common.util.CollectionUtils;

/**
 * @author wang.liang
 */
public interface Querier<T> {

    /**
     * Match data
     *
     * @param data the data
     * @return the boolean
     */
    <D extends T> boolean isMatch(D data);

    /**
     * Do count.
     *
     * @param list the list
     * @return the count
     */
    default <D extends T> int doCount(List<D> list) {
        int count = 0;
        if (CollectionUtils.isNotEmpty(list)) {
            for (D t : list) {
                if (this.isMatch(t)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Do filter.
     *
     * @param list the list
     * @return the list after filter
     */
    default <D extends T> List<D> doFilter(List<D> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        List<D> found = new ArrayList<>();
        for (D t : list) {
            if (this.isMatch(t)) {
                found.add(t);
            }
        }
        return found;
    }

    /**
     * Do sort.
     *
     * @param list the list
     * @return the list after sort
     */
    <D extends T> List<D> doSort(List<D> list);

    /**
     * Do paging.
     *
     * @param list the list
     * @return the list after paging
     */
    <D extends T> List<D> doPaging(List<D> list);

    /**
     * Do query.
     * doFilter + doSort + doPaging
     *
     * @param list the list
     * @return the list after query
     */
    default <D extends T> List<D> doQuery(List<D> list) {
        if (list == null) {
            return Collections.emptyList();
        }

        if (list.isEmpty()) {
            return list;
        }

        // do filter
        list = this.doFilter(list);
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        // do sort
        list = this.doSort(list);

        // do paging
        list = this.doPaging(list);

        return list;
    }
}
