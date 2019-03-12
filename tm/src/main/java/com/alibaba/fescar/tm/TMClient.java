/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.tm;

import com.alibaba.fescar.common.loader.EnhancedServiceLoader;
import com.alibaba.fescar.common.loader.EnhancedServiceNotFoundException;
import com.alibaba.fescar.core.rpc.netty.TmRpcClient;
import com.alibaba.fescar.metrics.Publisher;

/**
 * The type Tm client.
 */
public class TMClient {
  /**
   * Init.
   *
   * @param applicationId           the application id
   * @param transactionServiceGroup the transaction service group
   */
  public static void init(String applicationId, String transactionServiceGroup) {
    TmRpcClient tmRpcClient = TmRpcClient.getInstance(applicationId, transactionServiceGroup);
    tmRpcClient.init();

    //try to load metrics publisher
    try {
      EnhancedServiceLoader.load(Publisher.class);
    } catch (EnhancedServiceNotFoundException ignored) {
    }
  }
}
