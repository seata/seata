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
package io.seata.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The type Comparable utils test.
 *
 * @author wang.liang
 */
public class ComparableUtilsTest {

    /**
     * Test compare.
     */
    @Test
    public void test_compare() {
        assertEquals(ComparableUtils.compare(null, null), 0);
        assertEquals(ComparableUtils.compare(1, 1), 0);
        assertEquals(ComparableUtils.compare("1", "1"), 0);

        assertEquals(ComparableUtils.compare(null, 1), -1);
        assertEquals(ComparableUtils.compare(1, 2), -1);
        assertEquals(ComparableUtils.compare("1", "2"), -1);

        assertEquals(ComparableUtils.compare(1, null), 1);
        assertEquals(ComparableUtils.compare(2, 1), 1);
        assertEquals(ComparableUtils.compare("2", "1"), 1);
    }
}
