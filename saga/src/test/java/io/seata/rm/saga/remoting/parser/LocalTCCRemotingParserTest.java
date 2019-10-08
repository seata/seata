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
package io.seata.rm.saga.remoting.parser;

import io.seata.rm.saga.SagaAction;
import io.seata.rm.saga.SagaActionImpl;
import io.seata.rm.saga.remoting.RemotingDesc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * The type Local saga remoting parser test.
 *
 * @author zhangsen
 */
public class LocalTCCRemotingParserTest {

    /**
     * The Local saga remoting parser.
     */
    LocalSAGARemotingParser localSAGARemotingParser = new LocalSAGARemotingParser();

    /**
     * Test service parser.
     */
    @Test
    public void testServiceParser(){
        SagaActionImpl tccAction = new SagaActionImpl();

        boolean result = localSAGARemotingParser.isService(tccAction, "a");
        Assertions.assertTrue(result);
    }

    /**
     * Test reference parser.
     */
    @Test
    public void testReferenceParser(){
        SagaActionImpl tccAction = new SagaActionImpl();

        boolean result = localSAGARemotingParser.isReference(tccAction, "b");
        Assertions.assertTrue(result);
    }

    /**
     * Test service desc.
     */
    @Test
    public void testServiceDesc(){
        SagaActionImpl tccAction = new SagaActionImpl();

        RemotingDesc remotingDesc = localSAGARemotingParser.getServiceDesc(tccAction, "c");
        Assertions.assertNotNull(remotingDesc);

        Assertions.assertEquals(remotingDesc.getInterfaceClassName(), "io.seata.rm.saga.SagaAction");
        Assertions.assertEquals(remotingDesc.getInterfaceClass(), SagaAction.class);
        Assertions.assertEquals(remotingDesc.getTargetBean(), tccAction);
    }

}
