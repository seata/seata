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
package io.seata.tc.store;

/**
 * The interface Session storable.
 *
 * @author slievrly
 */
public interface SessionStorable {

    /**
     * Encode byte [ ].
     *
     * @return the byte [ ]
     */
    byte[] encode();

    /**
     * Decode.
     *
     * @param src the src
     */
    void decode(byte[] src);
}
