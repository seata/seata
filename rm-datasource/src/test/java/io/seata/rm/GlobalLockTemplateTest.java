package io.seata.rm;

import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.context.RootContext;
import io.seata.core.model.GlobalLockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author selfishlover
 */
class GlobalLockTemplateTest {

    private GlobalLockTemplate template = new GlobalLockTemplate();

    private GlobalLockConfig config1 = generateGlobalLockConfig();

    private GlobalLockConfig config2 = generateGlobalLockConfig();

    @BeforeEach
    void setUp() {
        assertFalse(RootContext.requireGlobalLock(), "initial global lock flag should be false");
        assertNull(GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                "initial global lock config should be null");
    }

    @Test
    void testSingle() {
        assertDoesNotThrow(() -> {
            template.execute(new GlobalLockExecutor() {
                @Override
                public Object execute() {
                    assertTrue(RootContext.requireGlobalLock(), "fail to bind global lock flag");
                    assertSame(config1, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                            "global lock config changed during execution");
                    return null;
                }

                @Override
                public GlobalLockConfig getGlobalLockConfig() {
                    return config1;
                }
            });
        });
    }

    @Test
    void testNested() {
        assertDoesNotThrow(() -> {
            template.execute(new GlobalLockExecutor() {
                @Override
                public Object execute() {
                    assertTrue(RootContext.requireGlobalLock(), "fail to bind global lock flag");
                    assertSame(config1, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                            "global lock config changed during execution");
                    assertDoesNotThrow(() -> {
                        template.execute(new GlobalLockExecutor() {
                            @Override
                            public Object execute() {
                                assertTrue(RootContext.requireGlobalLock(), "inner lost global lock flag");
                                assertSame(config2, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                                        "fail to set inner global lock config");
                                return null;
                            }

                            @Override
                            public GlobalLockConfig getGlobalLockConfig() {
                                return config2;
                            }
                        });
                    });
                    assertTrue(RootContext.requireGlobalLock(), "outer lost global lock flag");
                    assertSame(config1, GlobalLockConfigHolder.getCurrentGlobalLockConfig(),
                            "outer global lock config was not restored");
                    return null;
                }

                @Override
                public GlobalLockConfig getGlobalLockConfig() {
                    return config1;
                }
            });
        });
    }

    @AfterEach
    void tearDown() {
        assertFalse(RootContext.requireGlobalLock(), "fail to unbind global lock flag");
        assertNull(GlobalLockConfigHolder.getCurrentGlobalLockConfig(), "fail to clean global lock config");
    }

    private GlobalLockConfig generateGlobalLockConfig() {
        GlobalLockConfig config = new GlobalLockConfig();
        config.setLockRetryInternal(100);
        config.setLockRetryTimes(3);
        return config;
    }
}