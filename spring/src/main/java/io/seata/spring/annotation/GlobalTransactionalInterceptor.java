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
package io.seata.spring.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.rm.GlobalLockTemplate;
import io.seata.tm.TransactionManagerHolder;
import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.FailureHandler;
import io.seata.tm.api.TransactionalExecutor;
import io.seata.tm.api.TransactionalTemplate;
import io.seata.tm.api.transaction.NoRollbackRule;
import io.seata.tm.api.transaction.RollbackRule;
import io.seata.tm.api.transaction.TransactionInfo;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;


import static io.seata.core.constants.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static io.seata.core.constants.DefaultValues.DEFAULT_TM_DEGRADE_CHECK;
import static io.seata.core.constants.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES;
import static io.seata.core.constants.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_PERIOD;

/**
 * The type Global transactional interceptor.
 *
 * @author slievrly
 */
public class GlobalTransactionalInterceptor implements ConfigurationChangeListener, MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionalInterceptor.class);
    private static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();

    private final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();
    private final GlobalLockTemplate<Object> globalLockTemplate = new GlobalLockTemplate<>();
    private final FailureHandler failureHandler;
    private static volatile boolean disable;
    private static int degradeCheckPeriod;
    private static volatile boolean degradeCheck;
    private static int degradeCheckAllowTimes;
    private static volatile Integer degradeNum = 0;
    private static volatile Integer reachNum = 0;
    private static volatile ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), new NamedThreadFactory("degradeCheckWorker", 1, true));
    private static volatile ThreadPoolExecutor taskThreadPool = new ThreadPoolExecutor(0, 200, 60L,
        TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("asyncDegradeCheckWorker", 1, true));

    /**
     * Instantiates a new Global transactional interceptor.
     *
     * @param failureHandler
     *            the failure handler
     */
    public GlobalTransactionalInterceptor(FailureHandler failureHandler) {
        this.failureHandler = failureHandler == null ? DEFAULT_FAIL_HANDLER : failureHandler;
        disable = ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
            DEFAULT_DISABLE_GLOBAL_TRANSACTION);
        degradeCheck = ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.CLIENT_DEGRADE_CHECK,
            DEFAULT_TM_DEGRADE_CHECK);
        if (degradeCheck) {
            degradeCheckPeriod = ConfigurationFactory.getInstance()
                .getInt(ConfigurationKeys.CLIENT_DEGRADE_CHECK_PERIOD, DEFAULT_TM_DEGRADE_CHECK_PERIOD);
            degradeCheckAllowTimes = ConfigurationFactory.getInstance()
                .getInt(ConfigurationKeys.CLIENT_DEGRADE_CHECK_ALLOW_TIMES, DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES);
            ConfigurationFactory.getInstance().addConfigListener(ConfigurationKeys.CLIENT_DEGRADE_CHECK,
                (ConfigurationChangeListener)this);
            wakingUpThread();
        }
    }

    @Override
    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        Class<?> targetClass =
            methodInvocation.getThis() != null ? AopUtils.getTargetClass(methodInvocation.getThis()) : null;
        Method specificMethod = ClassUtils.getMostSpecificMethod(methodInvocation.getMethod(), targetClass);
        if (null != specificMethod && !specificMethod.getDeclaringClass().equals(Object.class)) {
            final Method method = BridgeMethodResolver.findBridgedMethod(specificMethod);
            final GlobalTransactional globalTransactionalAnnotation =
                getAnnotation(method, targetClass, GlobalTransactional.class);
            final GlobalLock globalLockAnnotation = getAnnotation(method, targetClass, GlobalLock.class);
            boolean localDisable = disable || (degradeCheck && degradeNum >= degradeCheckAllowTimes);
            if (!localDisable) {
                if (globalTransactionalAnnotation != null) {
                    return handleGlobalTransaction(methodInvocation, globalTransactionalAnnotation);
                } else if (globalLockAnnotation != null) {
                    return handleGlobalLock(methodInvocation);
                }
            }
        }
        return methodInvocation.proceed();
    }

    private Object handleGlobalLock(final MethodInvocation methodInvocation) throws Exception {
        return globalLockTemplate.execute(() -> {
            try {
                return methodInvocation.proceed();
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Object handleGlobalTransaction(final MethodInvocation methodInvocation,
        final GlobalTransactional globalTrxAnno) throws Throwable {
        boolean succeed = true;
        try {
            return transactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    return methodInvocation.proceed();
                }

                public String name() {
                    String name = globalTrxAnno.name();
                    if (!StringUtils.isNullOrEmpty(name)) {
                        return name;
                    }
                    return formatMethod(methodInvocation.getMethod());
                }

                @Override
                public TransactionInfo getTransactionInfo() {
                    TransactionInfo transactionInfo = new TransactionInfo();
                    transactionInfo.setTimeOut(globalTrxAnno.timeoutMills());
                    transactionInfo.setName(name());
                    transactionInfo.setPropagation(globalTrxAnno.propagation());
                    Set<RollbackRule> rollbackRules = new LinkedHashSet<>();
                    for (Class<?> rbRule : globalTrxAnno.rollbackFor()) {
                        rollbackRules.add(new RollbackRule(rbRule));
                    }
                    for (String rbRule : globalTrxAnno.rollbackForClassName()) {
                        rollbackRules.add(new RollbackRule(rbRule));
                    }
                    for (Class<?> rbRule : globalTrxAnno.noRollbackFor()) {
                        rollbackRules.add(new NoRollbackRule(rbRule));
                    }
                    for (String rbRule : globalTrxAnno.noRollbackForClassName()) {
                        rollbackRules.add(new NoRollbackRule(rbRule));
                    }
                    transactionInfo.setRollbackRules(rollbackRules);
                    return transactionInfo;
                }
            });
        } catch (TransactionalExecutor.ExecutionException e) {
            TransactionalExecutor.Code code = e.getCode();
            switch (code) {
                case RollbackDone:
                    throw e.getOriginalException();
                case BeginFailure:
                    succeed = false;
                    failureHandler.onBeginFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case CommitFailure:
                    succeed = false;
                    failureHandler.onCommitFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackFailure:
                    failureHandler.onRollbackFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackRetrying:
                    failureHandler.onRollbackRetrying(e.getTransaction(), e.getCause());
                    throw e.getCause();
                default:
                    throw new ShouldNeverHappenException(String.format("Unknown TransactionalExecutor.Code: %s", code));
            }
        } finally {
            if (degradeCheck) {
                addTaskDegradeCheck(succeed);
            }
        }
    }

    public <T extends Annotation> T getAnnotation(Method method, Class<?> targetClass, Class<T> annotationClass) {
        return Optional.ofNullable(method).map(m -> m.getAnnotation(annotationClass))
            .orElse(Optional.ofNullable(targetClass).map(t -> t.getAnnotation(annotationClass)).orElse(null));
    }

    private String formatMethod(Method method) {
        StringBuilder sb = new StringBuilder(method.getName()).append("(");

        Class<?>[] params = method.getParameterTypes();
        int in = 0;
        for (Class<?> clazz : params) {
            sb.append(clazz.getName());
            if (++in < params.length) {
                sb.append(", ");
            }
        }
        return sb.append(")").toString();
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION.equals(event.getDataId())) {
            LOGGER.info("{} config changed, old value:{}, new value:{}", ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                disable, event.getNewValue());
            disable = Boolean.parseBoolean(event.getNewValue().trim());
            if (!disable) {
                wakingUpThread();
            }
        } else if (ConfigurationKeys.CLIENT_DEGRADE_CHECK.equals(event.getDataId())) {
            boolean degradeCheckNow = Boolean.parseBoolean(event.getNewValue());
            if (!degradeCheck && degradeCheckNow != degradeCheck) {
                wakingUpThread();
            }
            degradeCheck = degradeCheckNow;
            if (!degradeCheck) {
                reachNum = 0;
                degradeNum = 0;
            }
        }
    }

    /**
     * auto upgrade service detection
     */
    private void startDegradeCheck() {
        executor.execute(() -> {
            while (true) {
                synchronized (this) {
                    if (degradeCheck && !disable) {
                        try {
                            String xid = TransactionManagerHolder.get().begin(null, null, "degradeCheck", 60000);
                            TransactionManagerHolder.get().commit(xid);
                            addTaskDegradeCheck(true);
                        } catch (Exception e) {
                            addTaskDegradeCheck(false);
                        }
                    } else {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("an unknown error occurred and the self - checking thread could not wait error: {}",
                                    e.getMessage());
                            }
                            break;
                        }
                    }
                }
                try {
                    Thread.sleep(degradeCheckPeriod);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void addTaskDegradeCheck(boolean succeed) {
        taskThreadPool.execute(() -> {
            onDegradeCheck(succeed);
        });
    }

    private synchronized void onDegradeCheck(boolean succeed) {
        if (succeed) {
            if (degradeNum >= degradeCheckAllowTimes) {
                reachNum++;
                if (reachNum >= degradeCheckAllowTimes) {
                    reachNum = 0;
                    degradeNum = 0;
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("the current global transaction has been restored");
                    }
                }
            } else if (degradeNum != 0) {
                degradeNum = 0;
            }
        } else {
            if (degradeNum < degradeCheckAllowTimes) {
                degradeNum++;
                if (degradeNum >= degradeCheckAllowTimes) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("the current global transaction has been automatically downgraded");
                    }
                }
            } else if (reachNum != 0) {
                reachNum = 0;
            }
        }
    }

    private void wakingUpThread() {
        synchronized (this) {
            if (degradeCheckPeriod > 0 && degradeCheckAllowTimes > 0) {
                if (executor.getActiveCount() <= 0) {
                    startDegradeCheck();
                } else {
                    this.notifyAll();
                }
            }
        }
    }
}
