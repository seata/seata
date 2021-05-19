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
package io.seata.core.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A unit test for {@link RetryStrategyInfo}
 *
 * @author wang.liang
 */
public class RetryStrategyInfoTest {

    @Test
    public void testIsExpired() {
        // global transaction begin 10 seconds ago
        long globalTransactionBeginTime = System.currentTimeMillis() - 10 * 1000;

        // retryExpire is null, no expired, false
        RetryStrategyInfo retryStrategy = new RetryStrategyInfo();
        Assertions.assertFalse(retryStrategy.isExpired(globalTransactionBeginTime));

        // retryExpire is 0, no expired, false
        retryStrategy.setRetryExpire(0);
        Assertions.assertFalse(retryStrategy.isExpired(globalTransactionBeginTime));

        // retryExpire is 10, true
        retryStrategy.setRetryExpire(5);
        Assertions.assertTrue(retryStrategy.isExpired(globalTransactionBeginTime));

        // retryExpire is 20, false
        retryStrategy.setRetryExpire(15);
        Assertions.assertFalse(retryStrategy.isExpired(globalTransactionBeginTime));
    }

    @Test
    public void testIsReachedMaxRetryCount() {
        // maxRetryCount is null, un limit, false
        RetryStrategyInfo retryStrategy = new RetryStrategyInfo();
        Assertions.assertFalse(retryStrategy.isReachedMaxRetryCount(100));

        // maxRetryCount is 0, un limit, false
        retryStrategy.setMaxRetryCount(0);
        Assertions.assertFalse(retryStrategy.isReachedMaxRetryCount(100));

        retryStrategy.setMaxRetryCount(3);

        // maxRetryCount is 3, branchRetryCount is 2, false
        Assertions.assertFalse(retryStrategy.isReachedMaxRetryCount(2));

        // maxRetryCount is 3, branchRetryCount is 3, true
        Assertions.assertTrue(retryStrategy.isReachedMaxRetryCount(3));

        // maxRetryCount is 3, branchRetryCount is 4, true
        Assertions.assertTrue(retryStrategy.isReachedMaxRetryCount(4));
    }

    @Test
    public void testNextRetryInterval() {
        int retryInterval = 10;
        int maxRetryInterval = 45;
        int[] retryIntervalPlan = new int[]{1, 10, 30, 50, 100};

        //mode S
        RetryStrategyInfo retryStrategy = new RetryStrategyInfo(retryInterval);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(3), retryInterval * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(4), retryInterval * 1000);

        //mode I, has max retry interval
        retryStrategy = new RetryStrategyInfo(retryInterval, maxRetryInterval);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(3), 3 * retryInterval * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(4), 4 * retryInterval * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(5), maxRetryInterval * 1000);

        //mode I, no max retry interval
        retryStrategy = new RetryStrategyInfo(retryInterval, null);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(3), 3 * retryInterval * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(4), 4 * retryInterval * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(100), 100 * retryInterval * 1000);

        //mode P
        retryStrategy = new RetryStrategyInfo(retryIntervalPlan);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(1), retryIntervalPlan[1 - 1] * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(4), retryIntervalPlan[4 - 1] * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(5), retryIntervalPlan[5 - 1] * 1000);
        Assertions.assertEquals(retryStrategy.nextRetryInterval(6), retryIntervalPlan[retryIntervalPlan.length - 1] * 1000);
    }

    @Test
    public void testToString() {
        //empty
        RetryStrategyInfo retryStrategy = new RetryStrategyInfo();
        Assertions.assertEquals(retryStrategy.toString(), "");

        //no mode
        retryStrategy = new RetryStrategyInfo();
        retryStrategy.setMaxRetryCount(1);
        Assertions.assertEquals(retryStrategy.toString(), "-|1,-");

        //no mode
        retryStrategy = new RetryStrategyInfo();
        retryStrategy.setRetryExpire(2);
        Assertions.assertEquals(retryStrategy.toString(), "-|-,2s");

        //no mode
        retryStrategy = new RetryStrategyInfo();
        retryStrategy.setMaxRetryCount(1);
        retryStrategy.setRetryExpire(2);
        Assertions.assertEquals(retryStrategy.toString(), "-|1,2s");

        //mode S
        retryStrategy = new RetryStrategyInfo(30, 60, null);
        Assertions.assertEquals(retryStrategy.toString(), "S,30s|60,-");

        //mode I, has max retry interval
        retryStrategy = new RetryStrategyInfo(20, 120, 0, 3600);
        Assertions.assertEquals(retryStrategy.toString(), "I,20s,2m|-,1h");

        //mode I, no max retry interval, no limit
        retryStrategy = new RetryStrategyInfo(1, null);
        Assertions.assertEquals(retryStrategy.toString(), "I,1s");

        //mode P, no limit
        retryStrategy = new RetryStrategyInfo(new int[]{1, 10, 30, 50, 100});
        Assertions.assertEquals(retryStrategy.toString(), "P,1s,10s,30s,50s,100s");
    }

    @Test
    public void testValueOf() {
        String retryStrategyStr = "";
        RetryStrategyInfo retryStrategy = new RetryStrategyInfo();
        retryStrategy.valueOf(retryStrategyStr);
        Assertions.assertEquals(retryStrategy.toString(), retryStrategyStr);


        retryStrategyStr = "S,10s|1,-";
        retryStrategy = new RetryStrategyInfo();
        retryStrategy.valueOf(retryStrategyStr);
        Assertions.assertEquals(retryStrategy.toString(), retryStrategyStr);

        retryStrategyStr = "I,1s,2s|3,4s";
        retryStrategy = new RetryStrategyInfo();
        retryStrategy.valueOf(retryStrategyStr);
        Assertions.assertEquals(retryStrategy.toString(), retryStrategyStr);

        retryStrategyStr = "I,1s|-,2s";
        retryStrategy = new RetryStrategyInfo();
        retryStrategy.valueOf(retryStrategyStr);
        Assertions.assertEquals(retryStrategy.toString(), retryStrategyStr);

        retryStrategyStr = "P,1s,10s,30s,50s,100s|0,-";
        retryStrategy = new RetryStrategyInfo();
        retryStrategy.valueOf(retryStrategyStr);
        Assertions.assertEquals(retryStrategy.toString(), "P,1s,10s,30s,50s,100s");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RetryStrategyInfo retryStrategy2 = new RetryStrategyInfo();
            retryStrategy2.valueOf("S,1,10,30,50,100|0,0");
        });
    }

    @Test
    public void testIsEmpty() {
        RetryStrategyInfo retryStrategy = new RetryStrategyInfo();
        Assertions.assertTrue(retryStrategy.isEmpty());

        retryStrategy.setMode("I");
        Assertions.assertFalse(retryStrategy.isEmpty());
    }
}
