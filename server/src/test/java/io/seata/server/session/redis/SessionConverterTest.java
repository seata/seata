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
package io.seata.server.session.redis;

import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;

/**
 * The session converter utils
 *
 * @author wangzhongxiang
 */
public class SessionConverterTest {

    @Test
    public void testConvertGlobalSessionNotNull() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Date date = new Date();
        long now = date.getTime();
        globalTransactionDO.setXid("192.168.158.80:8091:39372760251957248");
        globalTransactionDO.setTransactionId(39372760251957248L);
        globalTransactionDO.setStatus(1);
        globalTransactionDO.setApplicationId("credit");
        globalTransactionDO.setTransactionServiceGroup("fsp_tx");
        globalTransactionDO.setTransactionName("createOrder");
        globalTransactionDO.setTimeout(60);
        globalTransactionDO.setBeginTime(now);
        globalTransactionDO.setApplicationData("id=1,product=2");
        globalTransactionDO.setGmtCreate(date);
        globalTransactionDO.setGmtModified(date);

        GlobalSession globalSession = SessionConverter.convertGlobalSession(globalTransactionDO);
        Assertions.assertEquals(globalTransactionDO.getXid(),globalSession.getXid());
        Assertions.assertEquals(globalTransactionDO.getTransactionId(),globalSession.getTransactionId());
        Assertions.assertEquals(globalTransactionDO.getStatus(),globalSession.getStatus().getCode());
        Assertions.assertEquals(globalTransactionDO.getApplicationId(),globalSession.getApplicationId());
        Assertions.assertEquals(globalTransactionDO.getTransactionServiceGroup(),globalSession.getTransactionServiceGroup());
        Assertions.assertEquals(globalTransactionDO.getTransactionName(),globalSession.getTransactionName());
        Assertions.assertEquals(globalTransactionDO.getTimeout(),globalSession.getTimeout());
        Assertions.assertEquals(globalTransactionDO.getBeginTime(),globalSession.getBeginTime());
        Assertions.assertEquals(globalTransactionDO.getApplicationData(),globalSession.getApplicationData());
    }

    @Test
    public void testConvertGlobalSessionNull() {
        GlobalTransactionDO globalTransactionDO = null;
        GlobalSession globalSession = SessionConverter.convertGlobalSession(globalTransactionDO);
        Assertions.assertNull(globalSession);
    }

    @Test
    public void testConvertBranchSessionNotNull() {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        Date date = new Date();
        branchTransactionDO.setXid("192.168.158.80:8091:39372760251957248");
        branchTransactionDO.setTransactionId(39372760251957248L);
        branchTransactionDO.setBranchId(39372760251957111L);
        branchTransactionDO.setResourceGroupId("t1");
        branchTransactionDO.setResourceId("jdbc:mysql://116.62.62.62/seata-storage");
        branchTransactionDO.setBranchType(BranchType.AT.name());
        branchTransactionDO.setStatus(1);
        branchTransactionDO.setClientId("storage-server:192.168.158.80:11934");
        branchTransactionDO.setApplicationData("");
        branchTransactionDO.setGmtCreate(date);
        branchTransactionDO.setGmtModified(date);

        BranchSession branchSession = SessionConverter.convertBranchSession(branchTransactionDO);
        Assertions.assertEquals(branchTransactionDO.getXid(),branchSession.getXid());
        Assertions.assertEquals(branchTransactionDO.getTransactionId(),branchSession.getTransactionId());
        Assertions.assertEquals(branchTransactionDO.getBranchId(),branchSession.getBranchId());
        Assertions.assertEquals(branchTransactionDO.getResourceGroupId(),branchSession.getResourceGroupId());
        Assertions.assertEquals(branchTransactionDO.getResourceId(),branchSession.getResourceId());
        Assertions.assertEquals(branchTransactionDO.getBranchType(),branchSession.getBranchType().name());
        Assertions.assertEquals(branchTransactionDO.getStatus(),branchSession.getStatus().getCode());
        Assertions.assertEquals(branchTransactionDO.getClientId(),branchSession.getClientId());
        Assertions.assertEquals(branchTransactionDO.getApplicationData(),branchSession.getApplicationData());
    }

    @Test
    public void testConvertBranchSessionNull() {
        BranchTransactionDO branchTransactionDO = null;
        BranchSession branchSession = SessionConverter.convertBranchSession(branchTransactionDO);
        Assertions.assertNull(branchSession);
    }

    @Test
    public void testConvertGlobalTransactionDONotNull() {
        Date date = new Date();
        long now = date.getTime();
        GlobalSession globalSession = new GlobalSession("application1","fsp_tx","createOrder",60);
        globalSession.setXid("192.168.158.80:8091:39372760251957248");
        globalSession.setTransactionId(39372760251957248L);
        globalSession.setLocalStatus(GlobalStatus.Begin);
        globalSession.setBeginTime(now);
        globalSession.setApplicationData("id=1,product=2");
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);

        Assertions.assertEquals(globalSession.getXid(),globalTransactionDO.getXid());
        Assertions.assertEquals(globalSession.getTransactionId(),globalTransactionDO.getTransactionId());
        Assertions.assertEquals(globalSession.getStatus().getCode(),globalTransactionDO.getStatus());
        Assertions.assertEquals(globalSession.getTransactionName(),globalTransactionDO.getTransactionName());
        Assertions.assertEquals(globalSession.getApplicationId(),globalTransactionDO.getApplicationId());
        Assertions.assertEquals(globalSession.getTransactionServiceGroup(),globalTransactionDO.getTransactionServiceGroup());
        Assertions.assertEquals(globalSession.getTransactionName(),globalTransactionDO.getTransactionName());
        Assertions.assertEquals(globalSession.getTimeout(),globalTransactionDO.getTimeout());
        Assertions.assertEquals(globalSession.getBeginTime(),globalTransactionDO.getBeginTime());
        Assertions.assertEquals(globalSession.getApplicationData(),globalTransactionDO.getApplicationData());
    }

    @Test
    public void testConvertGlobalTransactionDONull() {
        GlobalSession globalSession = null;
        try {
            SessionConverter.convertGlobalTransactionDO(globalSession);
        } catch (Exception ex) {
            Assertions.assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testConvertBranchTransactionDONotNull() {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("192.168.158.80:8091:39372760251957248");
        branchSession.setResourceId("jdbc:mysql://116.62.62.62/seata-storage");
        branchSession.setTransactionId(39372760251957248L);
        branchSession.setBranchId(39372760251957111L);
        branchSession.setBranchType(BranchType.XA);
        branchSession.setResourceGroupId("abc");
        branchSession.setClientId("storage-server:192.168.158.80:11934");
        branchSession.setStatus(BranchStatus.PhaseOne_Failed);
        branchSession.setApplicationData("abc=123");

        BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
        Assertions.assertEquals(branchSession.getXid(),branchTransactionDO.getXid());
        Assertions.assertEquals(branchSession.getResourceId(),branchTransactionDO.getResourceId());
        Assertions.assertEquals(branchSession.getTransactionId(),branchTransactionDO.getTransactionId());
        Assertions.assertEquals(branchSession.getBranchId(),branchTransactionDO.getBranchId());
        Assertions.assertEquals(branchSession.getBranchType().name(),branchTransactionDO.getBranchType());
        Assertions.assertEquals(branchSession.getResourceGroupId(),branchTransactionDO.getResourceGroupId());
        Assertions.assertEquals(branchSession.getClientId(),branchTransactionDO.getClientId());
        Assertions.assertEquals(branchSession.getStatus().getCode(),branchTransactionDO.getStatus());
        Assertions.assertEquals(branchSession.getApplicationData(),branchTransactionDO.getApplicationData());
    }

    @Test
    public void testConvertBranchTransactionDONull() {
        BranchSession branchSession = null;
        try {
            SessionConverter.convertBranchTransactionDO(branchSession);
        } catch (Exception ex) {
            Assertions.assertTrue(ex instanceof IllegalArgumentException);
        }
    }
}
