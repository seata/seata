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
package io.seata.server.event;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.Subscribe;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.coordinator.DefaultCoordinatorTest;
import io.seata.tc.coordinator.DefaultCore;
import io.seata.tc.event.EventBusManager;
import io.seata.tc.session.SessionHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test events come from Default Core.
 *
 * @author zhengyangyong
 */
public class DefaultCoreForEventBusTest {
    @Test
    public void test() throws IOException, TransactionException, InterruptedException {
        class GlobalTransactionEventSubscriber {
            private final Map<GlobalStatus, AtomicInteger> eventCounters;

            public Map<GlobalStatus, AtomicInteger> getEventCounters() {
                return eventCounters;
            }

            public GlobalTransactionEventSubscriber() {
                this.eventCounters = new ConcurrentHashMap<>();
            }

            @Subscribe
            public void processGlobalTransactionEvent(GlobalTransactionEvent event) {
                AtomicInteger counter = eventCounters.computeIfAbsent(event.getStatus(),
                    status -> new AtomicInteger(0));
                counter.addAndGet(1);
            }
        }
        SessionHolder.init(null);
        DefaultCoordinator coordinator = new DefaultCoordinator(null);
        coordinator.init();
        try {
            DefaultCore core = new DefaultCore(new DefaultCoordinatorTest.MockServerMessageSender());

            GlobalTransactionEventSubscriber subscriber = new GlobalTransactionEventSubscriber();
            EventBusManager.get().register(subscriber);

            //start a transaction
            String xid = core.begin("test_app_id", "default_group", "test_tran_name", 30000);

            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.Begin).get());

            //commit this transaction
            core.commit(xid);

            //we need sleep for a short while because default canBeCommittedAsync() is true
            Thread.sleep(1000);

            //check
            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.AsyncCommitting).get());
            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.Committed).get());

            //start another new transaction
            xid = core.begin("test_app_id", "default_group", "test_tran_name2", 30000);

            Assertions.assertEquals(2, subscriber.getEventCounters().get(GlobalStatus.Begin).get());

            core.rollback(xid);

            //check
            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.Rollbacking).get());
            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.Rollbacked).get());

            //start more one new transaction for test timeout and let this transaction immediately timeout
            xid = core.begin("test_app_id", "default_group", "test_tran_name3", 0);

            //sleep for check ->  DefaultCoordinator.timeoutCheck
            Thread.sleep(1000);

            //at lease retry once because DefaultCoordinator.timeoutCheck is 1 second
            Assertions.assertTrue(subscriber.getEventCounters().get(GlobalStatus.TimeoutRollbacking).get() >= 1);
        } finally {
            coordinator.destroy();
            SessionHolder.destroy();
        }
    }
}
