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
package io.seata.server.coordinator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.seata.core.exception.TransactionException;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.metrics.Measurement;
import io.seata.server.metrics.MetricsManager;
import io.seata.tc.session.SessionHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test Metrics
 *
 * @author zhengyangyong
 */
public class DefaultCoordinatorMetricsTest {
    @Test
    public void test() throws IOException, TransactionException, InterruptedException {
        SessionHolder.init(null);
        DefaultCoordinator coordinator = new DefaultCoordinator(null);
        coordinator.init();
        try {
            MetricsManager.get().init();

            //start a transaction
            GlobalBeginRequest request = new GlobalBeginRequest();
            request.setTransactionName("test_transaction");
            GlobalBeginResponse response = new GlobalBeginResponse();
            coordinator.doGlobalBegin(request, response, new RpcContext());

            Map<String, Measurement> measurements = new HashMap<>();
            MetricsManager.get().getRegistry().measure().forEach(
                    measurement -> measurements.put(measurement.getId().toString(), measurement));

            Assertions.assertEquals(1, measurements.size());
            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=counter,role=tc,status=active)").getValue(), 0);

            //commit this transaction
            GlobalCommitRequest commitRequest = new GlobalCommitRequest();
            commitRequest.setXid(response.getXid());
            coordinator.doGlobalCommit(commitRequest, new GlobalCommitResponse(), new RpcContext());

            //we need sleep for a short while because default canBeCommittedAsync() is true
            Thread.sleep(200);

            measurements.clear();
            MetricsManager.get().getRegistry().measure().forEach(
                    measurement -> measurements.put(measurement.getId().toString(), measurement));
            Assertions.assertEquals(9, measurements.size());
            Assertions.assertEquals(0,
                    measurements.get("seata.transaction(meter=counter,role=tc,status=active)").getValue(), 0);
            Assertions
                    .assertEquals(1, measurements.get("seata.transaction(meter=counter,role=tc,status=committed)").getValue(),
                            0);
            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=summary,role=tc,statistic=count,status=committed)").getValue(),
                    0);
            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=summary,role=tc,statistic=total,status=committed)").getValue(),
                    0);
            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=timer,role=tc,statistic=count,status=committed)").getValue(), 0);

            //start another new transaction
            request = new GlobalBeginRequest();
            request.setTransactionName("test_transaction_2");
            response = new GlobalBeginResponse();
            coordinator.doGlobalBegin(request, response, new RpcContext());

            //rollback this transaction
            GlobalRollbackRequest rollbackRequest = new GlobalRollbackRequest();
            rollbackRequest.setXid(response.getXid());
            coordinator.doGlobalRollback(rollbackRequest, new GlobalRollbackResponse(), new RpcContext());

            Thread.sleep(200);

            measurements.clear();
            MetricsManager.get().getRegistry().measure().forEach(
                    measurement -> measurements.put(measurement.getId().toString(), measurement));
            Assertions.assertEquals(17, measurements.size());
            Assertions.assertEquals(0,
                    measurements.get("seata.transaction(meter=counter,role=tc,status=active)").getValue(), 0);

            Assertions
                    .assertEquals(1, measurements.get("seata.transaction(meter=counter,role=tc,status=committed)").getValue(),
                            0);
            Assertions.assertEquals(0,
                    measurements.get("seata.transaction(meter=summary,role=tc,statistic=count,status=committed)").getValue(),
                    0);
            Assertions.assertEquals(0,
                    measurements.get("seata.transaction(meter=summary,role=tc,statistic=total,status=committed)").getValue(),
                    0);
            Assertions.assertEquals(0,
                    measurements.get("seata.transaction(meter=timer,role=tc,statistic=count,status=committed)").getValue(), 0);

            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=counter,role=tc,status=rollbacked)").getValue(), 0);
            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=summary,role=tc,statistic=count,status=rollbacked)").getValue(),
                    0);
            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=summary,role=tc,statistic=total,status=rollbacked)").getValue(),
                    0);
            Assertions.assertEquals(1,
                    measurements.get("seata.transaction(meter=timer,role=tc,statistic=count,status=rollbacked)").getValue(), 0);
        } finally {
            coordinator.destroy();
            SessionHolder.destroy();
        }
    }

}
