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

/**
 * The type Comparable Utils.
 *
 * @author wang.liang
 */
public class ComparableUtils {

    private ComparableUtils() {
    }

    /**
     * Compare a and b.
     *
     * @param a the comparable object a
     * @param b the comparable object b
     * @return 0: equals    -1: a < b    1: a > b
     */
    public static int compare(Comparable a, Comparable b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }

        return a.compareTo(b);
    }
}
