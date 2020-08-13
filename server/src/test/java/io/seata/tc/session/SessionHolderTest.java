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
package io.seata.tc.session;

import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.StoreMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static io.seata.tc.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;

/**
 * The type Session holder test.
 *
 * @author Wu
 */
public class SessionHolderTest {
    private String pathname;

    @BeforeEach
    public void before() {
        String sessionStorePath = SessionHolder.CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR);
        //delete file previously created
        pathname = sessionStorePath + File.separator + ROOT_SESSION_MANAGER_NAME;
    }

    @Test
    public void testInit() throws IOException {
        File rootSessionFile = new File(pathname);
        if (rootSessionFile.exists()) {
            rootSessionFile.delete();
        }
        final String mode = StoreMode.FILE.getName();
        SessionHolder.init(mode);
        try {
            final File actual = new File(pathname);
            Assertions.assertTrue(actual.exists());
            Assertions.assertTrue(actual.isFile());
        } finally {
            SessionHolder.destroy();
        }
    }

    @AfterEach
    public void after() {
        final File actual = new File(pathname);
        if (actual.exists()) {
            actual.delete();
        }
    }
}
