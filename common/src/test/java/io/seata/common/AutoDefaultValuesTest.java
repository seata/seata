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
package io.seata.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type AutoDefaultValues test.
 *
 * @author wang.liang
 */
class AutoDefaultValuesTest {

    @Test
    void test_autoDefaultSagaJsonParser() {
        assertThat(AutoDefaultValues.autoDefaultSagaJsonParser()).isEqualTo("fastjson");
    }

    @Test
    void test_autoDefaultUndoLogSerialization() {
        assertThat(AutoDefaultValues.autoDefaultUndoLogSerialization()).isEqualTo("jackson");
    }

    @Test
    void test_hasFastjson() {
        assertThat(AutoDefaultValues.hasFastjson()).isEqualTo(false);
    }

    @Test
    void test_hasJackson() {
        assertThat(AutoDefaultValues.hasJackson()).isEqualTo(false);
    }

    @Test
    void test_hasKryo() {
        assertThat(AutoDefaultValues.hasKryo()).isEqualTo(false);
    }

    @Test
    void test_hasProtostuff() {
        assertThat(AutoDefaultValues.hasProtostuff()).isEqualTo(false);
    }
}
