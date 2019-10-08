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
package io.seata.rm.saga.remoting.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.saga.SAGAResource;
import io.seata.rm.saga.api.BusinessActionContext;
import io.seata.rm.saga.api.SagaCompensiable;
import io.seata.rm.saga.remoting.RemotingDesc;
import io.seata.rm.saga.remoting.RemotingParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * parsing remoting bean
 *
 * @author zhangsen
 */
public class DefaultRemotingParser {

    /**
     * all remoting bean parser
     */
    protected static List<RemotingParser> allRemotingParsers = new ArrayList<RemotingParser>();

    /**
     * all remoting beans beanName -> RemotingDesc
     */
    protected static Map<String, RemotingDesc> remotingServiceMap = new ConcurrentHashMap<String, RemotingDesc>();

    private static class SingletonHolder {
        private static DefaultRemotingParser INSTANCE = new DefaultRemotingParser();
    }

    /**
     * Get resource manager.
     *
     * @return the resource manager
     */
    public static DefaultRemotingParser get() {
        return DefaultRemotingParser.SingletonHolder.INSTANCE;
    }

    /**
     * Instantiates a new Default remoting parser.
     */
    protected DefaultRemotingParser() {
        initRemotingParser();
    }

    /**
     * init parsers
     */
    protected void initRemotingParser() {
        //init all resource managers
        List<RemotingParser> remotingParsers = EnhancedServiceLoader.loadAll(RemotingParser.class);
        if (CollectionUtils.isNotEmpty(remotingParsers)) {
            allRemotingParsers.addAll(remotingParsers);
        }
    }

    /**
     * is remoting bean ?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     */
    public boolean isRemoting(Object bean, String beanName) {
        for (RemotingParser remotingParser : allRemotingParsers) {
            if (remotingParser.isRemoting(bean, beanName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * is reference bean?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     */
    public boolean isReference(Object bean, String beanName) {
        for (RemotingParser remotingParser : allRemotingParsers) {
            if (remotingParser.isReference(bean, beanName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * is service bean ?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     */
    public boolean isService(Object bean, String beanName) {
        for (RemotingParser remotingParser : allRemotingParsers) {
            if (remotingParser.isService(bean, beanName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * get the remoting Service desc
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return service desc
     */
    public RemotingDesc getServiceDesc(Object bean, String beanName) {
        List<RemotingDesc> ret = new ArrayList<RemotingDesc>();
        for (RemotingParser remotingParser : allRemotingParsers) {
            RemotingDesc s = remotingParser.getServiceDesc(bean, beanName);
            if (s != null) {
                ret.add(s);
            }
        }
        if (ret.size() == 1) {
            return ret.get(0);
        } else if (ret.size() > 1) {
            throw new FrameworkException("More than one RemotingParser for bean:" + beanName);
        } else {
            return null;
        }
    }

    /**
     * parse the remoting bean info
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return remoting desc
     */
    public RemotingDesc parserRemotingServiceInfo(Object bean, String beanName) {
        RemotingDesc remotingBeanDesc = getServiceDesc(bean, beanName);
        if (remotingBeanDesc == null) {
            return null;
        }
        remotingServiceMap.put(beanName, remotingBeanDesc);

        Class<?> interfaceClass = remotingBeanDesc.getInterfaceClass();
        Method[] methods = interfaceClass.getMethods();
        if (isService(bean, beanName)) {
            try {
                //service bean， registry resource
                Object targetBean = remotingBeanDesc.getTargetBean();
                for (Method m : methods) {
                    SagaCompensiable sagaCompensiable = m.getAnnotation(SagaCompensiable.class);
                    if (sagaCompensiable != null) {
                        SAGAResource sagaResource = new SAGAResource();
                        sagaResource.setActionName(sagaCompensiable.name());
                        sagaResource.setTargetBean(targetBean);
                        sagaResource.setCommitMethod(m);
                        sagaResource.setRollbackMethodName(sagaCompensiable.rollback());
                        sagaResource.setRollbackMethod(ReflectionUtil
                            .getMethod(interfaceClass, sagaCompensiable.rollback(),
                                new Class[] {BusinessActionContext.class}));
                        //registry saga resource
                        DefaultResourceManager.get().registerResource(sagaResource);
                    }
                }
            } catch (Throwable t) {
                throw new FrameworkException(t, "parser remting service error");
            }
        }
        if (isReference(bean, beanName)) {
            //reference bean， TCC proxy
            remotingBeanDesc.setReference(true);
        }
        return remotingBeanDesc;
    }

    /**
     * Get remoting bean desc remoting desc.
     *
     * @param beanName the bean name
     * @return the remoting desc
     */
    public RemotingDesc getRemotingBeanDesc(String beanName) {
        return remotingServiceMap.get(beanName);
    }

}
