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
package io.seata.rm.saga.remoting;

/**
 * remoting protocols enum
 *
 * @author zhangsen
 */
public class Protocols {

    /**
     * sofa-rpc service
     */
    public static final short SOFA_RPC = 2;

    /**
     * dubbo service
     */
    public static final short DUBBO = 3;

    /**
     * restful service
     */
    public static final short RESTFUL = 4;

    /**
     * local bean
     */
    public static final short IN_JVM = 5;

    /**
     * hsf service
     */
    public static final short HSF = 8;

}
