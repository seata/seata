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
package io.seata.integration.thread;

import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.*;

public class ContextTest {

    static ExecutorService executorService;

    @BeforeAll
    public static void before() {
        executorService = new ThreadPoolExecutor(2, 2, 1, TimeUnit.HOURS, new LinkedBlockingQueue<>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "contextText-" + System.currentTimeMillis());
            }
        });
    }

    @Test
    public void testConfig() {
        System.setProperty(ConfigurationKeys.CLIENT_THREAD_PROPAGATE, "true");
        Assertions.assertEquals(ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.CLIENT_THREAD_PROPAGATE), true);
    }

    @Test
    public void threadTest() throws InterruptedException {
        System.setProperty(ConfigurationKeys.CLIENT_THREAD_PROPAGATE, "true");
        String[] results = new String[2];
        CountDownLatch countDownLatch = new CountDownLatch(2);
        RootContext.bind("test-context");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                results[0] = RootContext.getXID();
                countDownLatch.countDown();
            }
        });
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                results[1] = RootContext.getXID();
                countDownLatch.countDown();
                return null;
            }
        });

        countDownLatch.await();
        Assertions.assertArrayEquals(new String[]{"test-context", "test-context"}, results);
    }

    @Test
    public void threadConfigTest() throws InterruptedException {
        System.setProperty(ConfigurationKeys.CLIENT_THREAD_PROPAGATE, "false");
        String[] results = new String[2];
        CountDownLatch countDownLatch = new CountDownLatch(2);
        RootContext.bind("test-context");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                results[0] = RootContext.getXID();
                countDownLatch.countDown();
            }
        });
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                results[1] = RootContext.getXID();
                countDownLatch.countDown();
                return null;
            }
        });

        countDownLatch.await();
        Assertions.assertArrayEquals(new String[]{null, null}, results);
    }

    @Test
    public void nonPropagateRunnable() throws InterruptedException {
        System.setProperty(ConfigurationKeys.CLIENT_THREAD_PROPAGATE, "true");
        String[] results = new String[2];
        CountDownLatch countDownLatch = new CountDownLatch(2);

        RootContext.bind("none-context");
        executorService.submit(new NonPropagateRunnable() {
            @Override
            public void run() {
                results[0] = RootContext.getXID();
                countDownLatch.countDown();
            }
        });
        executorService.submit(new NonPropagateCallable<Object>() {
            @Override
            public Object call() throws Exception {
                results[1] = RootContext.getXID();
                countDownLatch.countDown();
                return null;
            }
        });

        countDownLatch.await();
        Assertions.assertNull(results[0]);
        Assertions.assertNull(results[1]);
    }
}
