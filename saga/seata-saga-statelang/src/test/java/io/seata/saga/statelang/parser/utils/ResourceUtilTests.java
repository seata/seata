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
package io.seata.saga.statelang.parser.utils;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResourceUtil tests
 *
 * @author wang.liang
 */
public class ResourceUtilTests {

    @Test
    public void getResources_test() {
        Resource[] resources = ResourceUtil.getResources("classpath*:statelang/*.json");
        assertThat(resources.length).isEqualTo(2);

        Resource[] resources2 = ResourceUtil.getResources(new String[]{"classpath*:statelang/*.json"});
        assertThat(resources2.length).isEqualTo(2);
    }
}