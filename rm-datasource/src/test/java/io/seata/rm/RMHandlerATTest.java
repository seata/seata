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
package io.seata.rm;

import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.rm.datasource.DataSourceManager;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.undo.UndoLogManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author selfishlover
 */
class RMHandlerATTest {

    @Test
    void hasUndoLogTableTest() {
        RMHandlerAT handler = buildHandler(true);
        UndoLogDeleteRequest request = buildRequest();
        int testTimes = 5;
        for (int i = 0; i < testTimes; i++) {
            handler.handle(request);
        }
        verify(handler, times(1)).checkUndoLogTableExist(any());
        verify(handler, times(testTimes)).deleteUndoLog(any(), any(), any());
    }

    @Test
    void noUndoLogTableTest() {
        RMHandlerAT handler = buildHandler(false);
        UndoLogDeleteRequest request = buildRequest();
        int testTimes = 5;
        for (int i = 0; i < testTimes; i++) {
            handler.handle(request);
        }
        verify(handler, times(1)).checkUndoLogTableExist(any());
        verify(handler, never()).deleteUndoLog(any(), any(), any());
    }

    private RMHandlerAT buildHandler(boolean hasUndoLogTable) {
        RMHandlerAT handler = spy(new RMHandlerAT());
        DataSourceManager dataSourceManager = mock(DataSourceManager.class);
        doReturn(dataSourceManager).when(handler).getResourceManager();

        DataSourceProxy dataSourceProxy = mock(DataSourceProxy.class);
        when(dataSourceManager.get(anyString())).thenReturn(dataSourceProxy);

        Connection connection = mock(Connection.class);
        assertDoesNotThrow(() -> {
            when(dataSourceProxy.getPlainConnection()).thenReturn(connection);
        });

        UndoLogManager manager = mock(UndoLogManager.class);
        when(manager.hasUndoLogTable(any())).thenReturn(hasUndoLogTable);
        doReturn(manager).when(handler).getUndoLogManager(any());

        return handler;
    }

    private UndoLogDeleteRequest buildRequest() {
        UndoLogDeleteRequest request = new UndoLogDeleteRequest();
        request.setResourceId("test");
        request.setSaveDays((short) 1);
        return request;
    }
}