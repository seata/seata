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

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.netty.RmNettyRemotingClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.rm.RMClient;
import io.seata.spring.annotation.scannercheckers.PackageScannerChecker;
import io.seata.spring.schema.SeataTarget;
import io.seata.spring.schema.SeataTargetHolder;
import io.seata.spring.schema.SeataTargetType;
import io.seata.spring.tcc.TccActionInterceptor;
import io.seata.spring.util.OrderUtil;
import io.seata.spring.util.SpringProxyUtils;
import io.seata.spring.util.TCCBeanParserUtils;
import io.seata.tm.TMClient;
import io.seata.tm.api.FailureHandler;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;

import static io.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;

/**
 * The type Global transaction scanner.
 *
 * @author slievrly
 */
public class GlobalTransactionScanner extends AbstractAutoProxyCreator
    implements ConfigurationChangeListener, InitializingBean, ApplicationContextAware, DisposableBean {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionScanner.class);

    private static final int AT_MODE = 1;
    private static final int MT_MODE = 2;

    private static final int ORDER_NUM = 1024;
    public static final int DEFAULT_MODE = AT_MODE + MT_MODE;

    private static final String SPRING_TRANSACTION_INTERCEPTOR_CLASS_NAME = "org.springframework.transaction.interceptor.TransactionInterceptor";

    private static final Set<String> PROXYED_SET = new HashSet<>();
    private static final Set<String> EXCLUDE_BEAN_NAME_SET = new HashSet<>();
    private static final Set<ScannerChecker> SCANNER_CHECKER_SET = new LinkedHashSet<>();

    private static ConfigurableListableBeanFactory beanFactory;

    private MethodInterceptor interceptor;
    private MethodInterceptor globalTransactionalInterceptor;

    private final String applicationId;
    private final String txServiceGroup;
    private final int mode;
    private String accessKey;
    private String secretKey;
    private volatile boolean disableGlobalTransaction = ConfigurationFactory.getInstance().getBoolean(
        ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final FailureHandler failureHandlerHook;

    private ApplicationContext applicationContext;

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param txServiceGroup the tx service group
     */
    public GlobalTransactionScanner(String txServiceGroup) {
        this(txServiceGroup, txServiceGroup, DEFAULT_MODE);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param txServiceGroup the tx service group
     * @param mode           the mode
     */
    public GlobalTransactionScanner(String txServiceGroup, int mode) {
        this(txServiceGroup, txServiceGroup, mode);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the default server group
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup) {
        this(applicationId, txServiceGroup, DEFAULT_MODE);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the tx service group
     * @param mode           the mode
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode) {
        this(applicationId, txServiceGroup, mode, null);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, FailureHandler failureHandlerHook) {
        this(applicationId, txServiceGroup, DEFAULT_MODE, failureHandlerHook);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param mode               the mode
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode,
                                    FailureHandler failureHandlerHook) {
        setOrder(ORDER_NUM);
        setProxyTargetClass(true);
        this.applicationId = applicationId;
        this.txServiceGroup = txServiceGroup;
        this.mode = mode;
        this.failureHandlerHook = failureHandlerHook;
    }

    /**
     * Sets access key.
     *
     * @param accessKey the access key
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Sets secret key.
     *
     * @param secretKey the secret key
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public void destroy() {
        ShutdownHook.getInstance().destroyAll();
    }

    private void initClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (StringUtils.isNullOrEmpty(applicationId) || StringUtils.isNullOrEmpty(txServiceGroup)) {
            throw new IllegalArgumentException(String.format("applicationId: %s, txServiceGroup: %s", applicationId, txServiceGroup));
        }
        //init TM
        TMClient.init(applicationId, txServiceGroup, accessKey, secretKey);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Transaction Manager Client is initialized. applicationId[{}] txServiceGroup[{}]", applicationId, txServiceGroup);
        }
        //init RM
        RMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Resource Manager is initialized. applicationId[{}] txServiceGroup[{}]", applicationId, txServiceGroup);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Global Transaction Clients are initialized. ");
        }
        registerSpringShutdownHook();

    }

    private void registerSpringShutdownHook() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).registerShutdownHook();
            ShutdownHook.removeRuntimeShutdownHook();
        }
        ShutdownHook.getInstance().addDisposable(TmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
        ShutdownHook.getInstance().addDisposable(RmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
    }

    /**
     * The following will be scanned, and added corresponding interceptor:
     * <p>
     * TM:
     *
     * @see io.seata.spring.annotation.GlobalTransactional // TM annotation
     * Corresponding interceptor:
     * @see io.seata.spring.annotation.GlobalTransactionalInterceptor#handleGlobalTransaction(MethodInvocation, GlobalTransactional) // TM handler
     * <p>
     * GlobalLock:
     * @see io.seata.spring.annotation.GlobalLock // GlobalLock annotation
     * Corresponding interceptor:
     * @see io.seata.spring.annotation.GlobalTransactionalInterceptor#handleGlobalLock(MethodInvocation, GlobalLock) // GlobalLock handler
     * <p>
     * TCC mode:
     * @see io.seata.rm.tcc.api.LocalTCC // TCC annotation on interface
     * @see io.seata.rm.tcc.api.TwoPhaseBusinessAction // TCC annotation on try method
     * @see io.seata.rm.tcc.remoting.RemotingParser // Remote TCC service parser
     * Corresponding interceptor:
     * @see io.seata.spring.tcc.TccActionInterceptor // the interceptor of TCC mode
     */
    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // do checkers
        if (!doCheckers(bean, beanName)) {
            return bean;
        }

        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.contains(beanName)) {
                    return bean;
                }
                interceptor = null;
                //check TCC proxy
                if (TCCBeanParserUtils.isTccAutoProxy(bean, beanName, applicationContext)) {
                    //TCC interceptor, proxy bean of sofa:reference/dubbo:reference, and LocalTCC
                    interceptor = new TccActionInterceptor(TCCBeanParserUtils.getRemotingDesc(beanName));
                    ConfigurationCache.addConfigListener(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                        (ConfigurationChangeListener) interceptor);
                } else {
                    Class<?> serviceInterface = SpringProxyUtils.findTargetClass(bean);
                    Class<?>[] interfacesIfJdk = SpringProxyUtils.findInterfaces(bean);

                    if (!existsAnnotation(new Class[]{serviceInterface}) && !existsAnnotation(interfacesIfJdk)) {
                        return bean;
                    }

                    if (globalTransactionalInterceptor == null) {
                        globalTransactionalInterceptor = new GlobalTransactionalInterceptor(failureHandlerHook);
                        ConfigurationCache.addConfigListener(
                            ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                            (ConfigurationChangeListener) globalTransactionalInterceptor);
                    }
                    interceptor = globalTransactionalInterceptor;
                }

                LOGGER.info("Bean[{}] with name [{}] would use interceptor [{}]", bean.getClass().getName(), beanName, interceptor.getClass().getName());
                if (!AopUtils.isAopProxy(bean)) {
                    bean = super.wrapIfNecessary(bean, beanName, cacheKey);
                } else {
                    AdvisedSupport advised = SpringProxyUtils.getAdvisedSupport(bean);
                    Advisor[] advisor = buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(null, null, null));
                    int pos;
                    for (Advisor avr : advisor) {
                        // Find the position based on the advisor's order, and add to advisors by pos
                        pos = findAddSeataAdvisorPosition(advised, avr);
                        advised.addAdvisor(pos, avr);
                    }
                }
                PROXYED_SET.add(beanName);
                return bean;
            }
        } catch (Exception exx) {
            throw new RuntimeException(exx);
        }
    }

    private boolean doCheckers(Object bean, String beanName) {
        if (PROXYED_SET.contains(beanName) || EXCLUDE_BEAN_NAME_SET.contains(beanName)
                || FactoryBean.class.isAssignableFrom(bean.getClass())) {
            return false;
        }

        if (!SCANNER_CHECKER_SET.isEmpty()) {
            for (ScannerChecker checker : SCANNER_CHECKER_SET) {
                try {
                    if (!checker.check(bean, beanName, beanFactory)) {
                        // failed check, do not scan this bean
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Do check failed: beanName={}, checker={}",
                            beanName, checker.getClass().getSimpleName(), e);
                }
            }
        }

        return true;
    }


    //region the methods about findAddSeataAdvisorPosition  START

    /**
     * Find pos for `advised.addAdvisor(pos, avr);`
     *
     * @param advised      the advised
     * @param seataAdvisor the seata advisor
     * @return the pos
     */
    private int findAddSeataAdvisorPosition(AdvisedSupport advised, Advisor seataAdvisor) {
        // Get seataAdvisor's order and interceptorPosition
        int seataOrder = OrderUtil.getOrder(seataAdvisor);
        SeataInterceptorPosition seataInterceptorPosition = getSeataInterceptorPosition(seataAdvisor);

        // If the interceptorPosition is any, check lowest or highest.
        if (SeataInterceptorPosition.Any == seataInterceptorPosition) {
            if (seataOrder == Ordered.LOWEST_PRECEDENCE) {
                // the last position
                return advised.getAdvisors().length;
            } else if (seataOrder == Ordered.HIGHEST_PRECEDENCE) {
                // the first position
                return 0;
            }
        } else {
            // If the interceptorPosition is not any, compute position if has TransactionInterceptor.
            Integer position = computePositionIfHasTransactionInterceptor(advised, seataAdvisor, seataInterceptorPosition, seataOrder);
            if (position != null) {
                // the position before or after TransactionInterceptor
                return position;
            }
        }

        // Find position
        return this.findPositionInAdvisors(advised.getAdvisors(), seataAdvisor);
    }

    @Nullable
    private Integer computePositionIfHasTransactionInterceptor(AdvisedSupport advised, Advisor seataAdvisor, SeataInterceptorPosition seataInterceptorPosition, int seataOrder) {
        // Find the TransactionInterceptor's advisor, order and position
        Advisor otherAdvisor = null;
        Integer transactionInterceptorPosition = null;
        Integer transactionInterceptorOrder = null;
        for (int i = 0, l = advised.getAdvisors().length; i < l; ++i) {
            otherAdvisor = advised.getAdvisors()[i];
            if (isTransactionInterceptor(otherAdvisor)) {
                transactionInterceptorPosition = i;
                transactionInterceptorOrder = OrderUtil.getOrder(otherAdvisor);
                break;
            }
        }
        // If the TransactionInterceptor does not exist, return null
        if (transactionInterceptorPosition == null) {
            return null;
        }

        // Reset seataOrder if the seataOrder is not match the position
        Advice seataAdvice = seataAdvisor.getAdvice();
        if (SeataInterceptorPosition.AfterTransaction == seataInterceptorPosition && OrderUtil.higherThan(seataOrder, transactionInterceptorOrder)) {
            int newSeataOrder = OrderUtil.lower(transactionInterceptorOrder, 1);
            ((SeataInterceptor)seataAdvice).setOrder(newSeataOrder);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("The {}'s order '{}' is higher or equals than {}'s order '{}' , reset {}'s order to lower order '{}'.",
                        seataAdvice.getClass().getSimpleName(), seataOrder,
                        otherAdvisor.getAdvice().getClass().getSimpleName(), transactionInterceptorOrder,
                        seataAdvice.getClass().getSimpleName(), newSeataOrder);
            }
            // the position after the TransactionInterceptor's advisor
            return transactionInterceptorPosition + 1;
        } else if (SeataInterceptorPosition.BeforeTransaction == seataInterceptorPosition && OrderUtil.lowerThan(seataOrder, transactionInterceptorOrder)) {
            int newSeataOrder = OrderUtil.higher(transactionInterceptorOrder, 1);
            ((SeataInterceptor)seataAdvice).setOrder(newSeataOrder);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("The {}'s order '{}' is lower or equals than {}'s order '{}' , reset {}'s order to higher order '{}'.",
                        seataAdvice.getClass().getSimpleName(), seataOrder,
                        otherAdvisor.getAdvice().getClass().getSimpleName(), transactionInterceptorOrder,
                        seataAdvice.getClass().getSimpleName(), newSeataOrder);
            }
            // the position before the TransactionInterceptor's advisor
            return transactionInterceptorPosition;
        }

        return null;
    }

    private int findPositionInAdvisors(Advisor[] advisors, Advisor seataAdvisor) {
        Advisor advisor;
        for (int i = 0, l = advisors.length; i < l; ++i) {
            advisor = advisors[i];
            if (OrderUtil.higherOrEquals(seataAdvisor, advisor)) {
                // the position before the current advisor
                return i;
            }
        }

        // the last position, after all the advisors
        return advisors.length;
    }

    private SeataInterceptorPosition getSeataInterceptorPosition(Advisor seataAdvisor) {
        Advice seataAdvice = seataAdvisor.getAdvice();
        if (seataAdvice instanceof SeataInterceptor) {
            return ((SeataInterceptor)seataAdvice).getPosition();
        } else {
            return SeataInterceptorPosition.Any;
        }
    }

    private boolean isTransactionInterceptor(Advisor advisor) {
        return SPRING_TRANSACTION_INTERCEPTOR_CLASS_NAME.equals(advisor.getAdvice().getClass().getName());
    }

    //endregion the methods about findAddSeataAdvisorPosition  END


    private boolean existsAnnotation(Class<?>[] classes) {
        if (CollectionUtils.isNotEmpty(classes)) {
            for (Class<?> clazz : classes) {
                if (clazz == null) {
                    continue;
                }
                GlobalTransactional trxAnno = clazz.getAnnotation(GlobalTransactional.class);
                if (trxAnno != null) {
                    return true;
                }
                SeataTarget seataClassTarget = SeataTargetHolder.INSTANCE.find(SeataTargetType.CLASS, clazz.getName());
                if (seataClassTarget != null) {
                    return true;
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    trxAnno = method.getAnnotation(GlobalTransactional.class);
                    if (trxAnno != null) {
                        return true;
                    }
                    SeataTarget seataMethodTarget = SeataTargetHolder.INSTANCE.find(SeataTargetType.METHOD, method.getName());
                    if (seataMethodTarget != null) {
                        return true;
                    }
                    GlobalLock lockAnno = method.getAnnotation(GlobalLock.class);
                    if (lockAnno != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private MethodDesc makeMethodDesc(GlobalTransactional anno, Method method) {
        return new MethodDesc(anno, method);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class beanClass, String beanName, TargetSource customTargetSource)
        throws BeansException {
        return new Object[]{interceptor};
    }

    @Override
    public void afterPropertiesSet() {
        if (disableGlobalTransaction) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Global transaction is disabled.");
            }
            ConfigurationCache.addConfigListener(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                (ConfigurationChangeListener) this);
            return;
        }
        if (initialized.compareAndSet(false, true)) {
            initClient();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.setBeanFactory(applicationContext);
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION.equals(event.getDataId())) {
            disableGlobalTransaction = Boolean.parseBoolean(event.getNewValue().trim());
            if (!disableGlobalTransaction && initialized.compareAndSet(false, true)) {
                LOGGER.info("{} config changed, old value:{}, new value:{}", ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                    disableGlobalTransaction, event.getNewValue());
                initClient();
            }
        }
    }

    public static void setBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        GlobalTransactionScanner.beanFactory = beanFactory;
    }

    public static void addScannablePackages(String... packages) {
        PackageScannerChecker.addScannablePackages(packages);
    }

    public static void addScannerCheckers(Collection<ScannerChecker> scannerCheckers) {
        if (CollectionUtils.isNotEmpty(scannerCheckers)) {
            scannerCheckers.remove(null);
            SCANNER_CHECKER_SET.addAll(scannerCheckers);
        }
    }

    public static void addScannerCheckers(ScannerChecker... scannerCheckers) {
        if (ArrayUtils.isNotEmpty(scannerCheckers)) {
            addScannerCheckers(Arrays.asList(scannerCheckers));
        }
    }

    public static void addScannerExcludeBeanNames(String... beanNames) {
        if (ArrayUtils.isNotEmpty(beanNames)) {
            EXCLUDE_BEAN_NAME_SET.addAll(Arrays.asList(beanNames));
        }
    }
}
