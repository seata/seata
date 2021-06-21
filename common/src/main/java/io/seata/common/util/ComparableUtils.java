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
public final class ComparableUtils {

    private ComparableUtils() {
    }

    /**
     * Compare a and b.
     *
     * @param a the comparable object a
     * @param b the comparable object b
     * @return 0: equals    -1: a < b    1: a > b
     */
    public static <T extends Comparable> int compare(T a, T b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }

        return a.compareTo(b);
    }

    /**
     * Compare a and b.
     * This overload method to avoid frequent packaging of primitive data.
     *
     * @param a the comparable object a
     * @param b the comparable object b
     * @return 0: equals    -1: a < b    1: a > b
     */
    public static int compare(int a, int b) {
        if (a == b) {
            return 0;
        } else if (a < b) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Compare a and b.
     * This overload method to avoid frequent packaging of primitive data.
     *
     * @param a the comparable object a
     * @param b the comparable object b
     * @return 0: equals    -1: a < b    1: a > b
     */
    public static int compare(long a, long b) {
        if (a == b) {
            return 0;
        } else if (a < b) {
            return -1;
        } else {
            return 1;
        }
    }
}
