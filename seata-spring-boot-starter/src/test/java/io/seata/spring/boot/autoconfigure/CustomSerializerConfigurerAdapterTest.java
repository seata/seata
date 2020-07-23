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
package io.seata.spring.boot.autoconfigure;

import com.esotericsoftware.kryo.Kryo;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.rm.datasource.undo.parser.CustomSerializerConfigurerAdapter;
import io.seata.rm.datasource.undo.parser.KryoConfigurerAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = CustomSerializerConfigurerAdapterTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ImportAutoConfiguration(SeataAutoConfiguration.class)
@SpringBootApplication
public class CustomSerializerConfigurerAdapterTest {

    @Configuration
    public static class TestConfig {

        @Bean
        public TestKryoConfigurerAdapter kryoConfigurerAdapter() {
            return Mockito.spy(new TestKryoConfigurerAdapter());
        }
    }

    public static class TestKryoConfigurerAdapter extends KryoConfigurerAdapter {

        @Override
        public void config(Kryo kryo) {
            super.config(kryo);
        }
    }

    @Test
    public void test() {
        UndoLogParser undoLogParser = EnhancedServiceLoader.load(UndoLogParser.class, "kryo");
        byte[] encode = undoLogParser.encode(new BranchUndoLog());
        BranchUndoLog decode = undoLogParser.decode(encode);
        TestKryoConfigurerAdapter adapter = (TestKryoConfigurerAdapter) CustomSerializerConfigurerAdapter.getConfig("kryo");
        Mockito.verify(adapter).config(Mockito.any());
    }
}
