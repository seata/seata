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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.seata.core.store.GlobalTableField.BEGIN_TIME;
import static io.seata.core.store.GlobalTableField.STATUS;
import static io.seata.core.store.GlobalTableField.TIMEOUT;

/**
 * @author wang.liang
 */
public class GlobalConditionTest {

    @Test
    public void test_isMatch() throws InterruptedException {
        GlobalCondition condition = new GlobalCondition();

        GlobalTransactionDO obj = new GlobalTransactionDO();

        // condition1: statuses
        condition.setStatuses(GlobalStatus.Finished);
        obj.setStatus(GlobalStatus.Begin.getCode());
        Assertions.assertFalse(condition.isMatch(obj));
        condition.setStatuses(GlobalStatus.Begin);
        Assertions.assertTrue(condition.isMatch(obj));

        // condition2: isTimeoutData
        condition.setNotTimeoutDataCondition();
        obj.setBeginTime(System.currentTimeMillis());
        obj.setTimeout(10);
        Assertions.assertTrue(condition.isMatch(obj));
        Thread.sleep(12);
        Assertions.assertFalse(condition.isMatch(obj));
        condition.setTimeoutDataCondition();
        Assertions.assertTrue(condition.isMatch(obj));

        // condition3: overTimeAliveMills
        condition.setOverTimeAliveMills(10);
        obj.setBeginTime(System.currentTimeMillis());
        Assertions.assertFalse(condition.isMatch(obj));
        Thread.sleep(12);
        Assertions.assertTrue(condition.isMatch(obj));

        // condition4: minGmtModified
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
    public void test_doCount_doFilter() throws InterruptedException {
        GlobalCondition condition = new GlobalCondition();
        condition.setStatuses(GlobalStatus.Finished);
        condition.setOverTimeAliveMills(10);
        condition.setTimeoutDataCondition();

        List<GlobalTransactionDO> list = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin.getCode());
        obj.setBeginTime(System.currentTimeMillis());
        obj.setTimeout(100);
        list.add(obj);

        // do filter
        int count1 = condition.doCount(list);
        List<GlobalTransactionDO> list1 = condition.doFilter(list);
        Assertions.assertEquals(count1, 0);
        Assertions.assertEquals(list1.size(), 0);

        Thread.sleep(102);
        count1 = condition.doCount(list);
        list1 = condition.doFilter(list);
        Assertions.assertEquals(count1, 0);
        Assertions.assertEquals(list1.size(), 0);

        condition.setStatuses(GlobalStatus.Begin);
        count1 = condition.doCount(list);
        list1 = condition.doFilter(list);
        Assertions.assertEquals(count1, 1);
        Assertions.assertEquals(list1.size(), 1);
    }

    @Test
    public void test_doSort() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Finished.getCode());
        obj.setTimeout(0);
        obj.setBeginTime(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.AsyncCommitting.getCode());
        obj.setTimeout(1);
        obj.setBeginTime(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin.getCode());
        obj.setTimeout(0);
        obj.setBeginTime(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.UnKnown.getCode());
        obj.setTimeout(1);
        obj.setBeginTime(1);
        list0.add(obj);

        int size = list0.size();

        // set sort fields
        GlobalCondition condition = new GlobalCondition();
        condition.setSortFields(TIMEOUT, BEGIN_TIME, STATUS); // sort by multi fields

        // do sort
        List<GlobalTransactionDO> list1 = condition.doSort(list0);

        // check
        Assertions.assertEquals(list1.size(), size);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(1).getStatus(), GlobalStatus.Finished.getCode());
        Assertions.assertEquals(list1.get(2).getStatus(), GlobalStatus.AsyncCommitting.getCode());
        Assertions.assertEquals(list1.get(3).getStatus(), GlobalStatus.UnKnown.getCode());
    }

    @Test
    public void test_doPaging() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin.getCode());
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.UnKnown.getCode());
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Finished.getCode());
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.AsyncCommitting.getCode());
        list0.add(obj);

        GlobalCondition condition = new GlobalCondition();
        condition.setPageIndex(2);
        condition.setPageSize(3);
        List<GlobalTransactionDO> list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 1);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.AsyncCommitting.getCode());

        condition.setPageIndex(3);
        condition.setPageSize(3);
        list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 0);

        condition.setPageIndex(1);
        condition.setPageSize(4);
        list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 4);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(1).getStatus(), GlobalStatus.UnKnown.getCode());
        Assertions.assertEquals(list1.get(2).getStatus(), GlobalStatus.Finished.getCode());
        Assertions.assertEquals(list1.get(3).getStatus(), GlobalStatus.AsyncCommitting.getCode());
    }

    @Test
    public void test_doQuery() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin.getCode());
        obj.setTimeout(1);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin.getCode());
        obj.setTimeout(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Finished.getCode());
        obj.setTimeout(0);
        list0.add(obj);

        obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.AsyncCommitting.getCode());
        obj.setTimeout(0);
        list0.add(obj);

        GlobalCondition condition = new GlobalCondition();
        // condition
        condition.setStatuses(GlobalStatus.Begin);
        // sort params
        condition.setSortFields(STATUS, TIMEOUT);
        // paging params
        condition.setPageIndex(1);
        condition.setPageSize(2);

        // do query
        List<GlobalTransactionDO> list1 = condition.doQuery(list0);
        Assertions.assertEquals(list1.size(), 2);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(0).getTimeout(), 0);
        Assertions.assertEquals(list1.get(1).getTimeout(), 1);

        // change condition
        condition.setStatuses(GlobalStatus.Finished);
        // do query
        list1 = condition.doQuery(list0);
        Assertions.assertEquals(list1.size(), 1);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.Finished.getCode());

        // change condition
        condition.setStatuses(GlobalStatus.Committed);
        // do query
        list1 = condition.doQuery(list0);
        Assertions.assertEquals(list1.size(), 0);
    }


    @Test
    public void test_getFromIndex_getToIndex() {
        GlobalCondition condition = new GlobalCondition();
        condition.setPageIndex(2);
        condition.setPageSize(2);

        int fromIndex = condition.getFromIndex();
        int toIndex = condition.getToIndex(fromIndex);

        Assertions.assertEquals(fromIndex, 2);
        Assertions.assertEquals(toIndex, 4);
    }
}
