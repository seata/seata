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

import io.seata.core.model.GlobalStatus;
import io.seata.core.store.querier.GlobalSessionCondition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.seata.core.store.standard.GlobalTableField.BEGIN_TIME;
import static io.seata.core.store.standard.GlobalTableField.STATUS;
import static io.seata.core.store.standard.GlobalTableField.TIMEOUT;

/**
 * @author wang.liang
 */
class GlobalSessionConditionTest {
    private static final String DEFAULT_XID = "1234567890";

    @Test
    void test_isMatch() throws InterruptedException {
        GlobalSessionCondition condition = new GlobalSessionCondition();

        GlobalTransactionDO obj = new GlobalTransactionDO();

        // condition: xid
        condition.setXid(DEFAULT_XID);
        Assertions.assertFalse(condition.isMatch(obj));
        obj.setXid(DEFAULT_XID);
        Assertions.assertTrue(condition.isMatch(obj));

        // condition: statuses
        condition.setStatuses(GlobalStatus.Finished);
        obj.setStatus(GlobalStatus.Begin);
        Assertions.assertFalse(condition.isMatch(obj));
        condition.setStatuses(GlobalStatus.Begin);
        Assertions.assertTrue(condition.isMatch(obj));

        // condition: isTimeoutData
        condition.setTimeoutData(false);
        obj.setBeginTime(System.currentTimeMillis());
        obj.setTimeout(10);
        Assertions.assertTrue(condition.isMatch(obj));
        Thread.sleep(12);
        Assertions.assertFalse(condition.isMatch(obj));
        condition.setTimeoutData(true);
        Assertions.assertTrue(condition.isMatch(obj));

        // condition: overTimeAliveMills
        condition.setOverTimeAliveMills(10);
        obj.setBeginTime(System.currentTimeMillis());
        Assertions.assertFalse(condition.isMatch(obj));
        Thread.sleep(12);
        Assertions.assertTrue(condition.isMatch(obj));

        // condition: minGmtModified
        condition.setMinGmtModified(new Date());
        obj.setGmtModified(null);
        Assertions.assertFalse(condition.isMatch(obj));
        obj.setGmtModified(new Date(condition.getMinGmtModified().getTime() - 1));
        Assertions.assertFalse(condition.isMatch(obj));
        obj.setGmtModified(condition.getMinGmtModified());
        Assertions.assertTrue(condition.isMatch(obj));
        obj.setGmtModified(new Date(condition.getMinGmtModified().getTime() + 1));
        Assertions.assertTrue(condition.isMatch(obj));
    }

    @Test
    void test_doCount_doFilter() throws InterruptedException {
        GlobalSessionCondition condition = new GlobalSessionCondition();
        condition.setStatuses(GlobalStatus.Finished);
        condition.setOverTimeAliveMills(10);
        condition.setTimeoutData(true);

        List<GlobalTransactionDO> list = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin);
        obj.setBeginTime(System.currentTimeMillis());
        obj.setTimeout(100);
        list.add(obj);

        // do count
        int count1 = condition.doCount(list);
        // do filter
        List<GlobalTransactionDO> list1 = condition.doFilter(list);
        Assertions.assertEquals(count1, 0);
        Assertions.assertEquals(list1.size(), 0);

        // sleep and make the data timeout
        Thread.sleep(102);
        // do count
        count1 = condition.doCount(list);
        // do filter
        list1 = condition.doFilter(list);
        Assertions.assertEquals(count1, 0);
        Assertions.assertEquals(list1.size(), 0);

        condition.setStatuses(GlobalStatus.Begin);
        // do count
        count1 = condition.doCount(list);
        // do filter
        list1 = condition.doFilter(list);
        Assertions.assertEquals(count1, 1);
        Assertions.assertEquals(list1.size(), 1);
    }

    @Test
    void test_doSort() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Finished);
        obj.setTimeout(0);
        obj.setBeginTime(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.AsyncCommitting);
        obj.setTimeout(1);
        obj.setBeginTime(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin);
        obj.setTimeout(0);
        obj.setBeginTime(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.UnKnown);
        obj.setTimeout(1);
        obj.setBeginTime(1);
        list0.add(obj);

        int size = list0.size();

        // set sort fields
        GlobalSessionCondition condition = new GlobalSessionCondition();
        condition.setSortFields(TIMEOUT, BEGIN_TIME, STATUS); // sort by multi fields

        // do sort
        List<GlobalTransactionDO> list1 = condition.doSort(list0);

        // check
        Assertions.assertEquals(list1.size(), size);
        Assertions.assertEquals(list1.get(0).getStatusCode(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(1).getStatusCode(), GlobalStatus.Finished.getCode());
        Assertions.assertEquals(list1.get(2).getStatusCode(), GlobalStatus.AsyncCommitting.getCode());
        Assertions.assertEquals(list1.get(3).getStatusCode(), GlobalStatus.UnKnown.getCode());
    }

    @Test
    void test_doPaging() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.UnKnown);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Finished);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.AsyncCommitting);
        list0.add(obj);

        GlobalSessionCondition condition = new GlobalSessionCondition();
        condition.setPageNumber(2);
        condition.setPageSize(3);
        List<GlobalTransactionDO> list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 1);
        Assertions.assertEquals(list1.get(0).getStatusCode(), GlobalStatus.AsyncCommitting.getCode());

        condition.setPageNumber(3);
        condition.setPageSize(3);
        list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 0);

        condition.setPageNumber(1);
        condition.setPageSize(4);
        list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 4);
        Assertions.assertEquals(list1.get(0).getStatusCode(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(1).getStatusCode(), GlobalStatus.UnKnown.getCode());
        Assertions.assertEquals(list1.get(2).getStatusCode(), GlobalStatus.Finished.getCode());
        Assertions.assertEquals(list1.get(3).getStatusCode(), GlobalStatus.AsyncCommitting.getCode());
    }

    @Test
    void test_doQuery() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin);
        obj.setTimeout(1);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin);
        obj.setTimeout(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Finished);
        obj.setTimeout(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.AsyncCommitting);
        obj.setTimeout(0);
        list0.add(obj);

        GlobalSessionCondition condition = new GlobalSessionCondition();
        // condition
        condition.setStatuses(GlobalStatus.Begin);
        // sort params
        condition.setSortFields(STATUS, TIMEOUT);
        // paging params
        condition.setPageNumber(1);
        condition.setPageSize(2);

        // do query
        List<GlobalTransactionDO> list1 = condition.doQuery(list0);
        Assertions.assertEquals(list1.size(), 2);
        Assertions.assertEquals(list1.get(0).getStatusCode(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(0).getTimeout(), 0);
        Assertions.assertEquals(list1.get(1).getTimeout(), 1);

        // change condition
        condition.setStatuses(GlobalStatus.Finished);
        // do query
        list1 = condition.doQuery(list0);
        Assertions.assertEquals(list1.size(), 1);
        Assertions.assertEquals(list1.get(0).getStatusCode(), GlobalStatus.Finished.getCode());

        // change condition
        condition.setStatuses(GlobalStatus.Committed);
        // do query
        list1 = condition.doQuery(list0);
        Assertions.assertEquals(list1.size(), 0);
    }


    @Test
    void test_getFromIndex_getToIndex() {
        GlobalSessionCondition condition = new GlobalSessionCondition();

        condition.setPageNumber(2);
        condition.setPageSize(2);
        int fromIndex = condition.getFromIndex();
        int toIndex = condition.getToIndex(fromIndex);
        Assertions.assertEquals(fromIndex, 2);
        Assertions.assertEquals(toIndex, 4);

        condition.setPageNumber(6);
        condition.setPageSize(7);
        fromIndex = condition.getFromIndex();
        toIndex = condition.getToIndex(fromIndex);
        Assertions.assertEquals(fromIndex, 35);
        Assertions.assertEquals(toIndex, 42);
    }
}
