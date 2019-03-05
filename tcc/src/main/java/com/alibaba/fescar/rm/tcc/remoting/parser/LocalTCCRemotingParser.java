package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.util.ProxyUtils;
import com.alibaba.fescar.rm.tcc.api.LocalTCC;
import com.alibaba.fescar.rm.tcc.remoting.Protocols;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;

import java.util.Set;

/**
 * 本地bean，非远程服务bean
 * @author zhangsen
 */
public class LocalTCCRemotingParser extends AbstractedRemotingParser {

    @Override
    public boolean isReference(Object bean, String beanName)  {
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ProxyUtils.getInterfaces(classType);
        for(Class<?> interClass : interfaceClasses){
            if(interClass.isAnnotationPresent(LocalTCC.class)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ProxyUtils.getInterfaces(classType);
        for(Class<?> interClass : interfaceClasses){
            if(interClass.isAnnotationPresent(LocalTCC.class)){
                return true;
            }
        }
        return false;
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        RemotingDesc remotingDesc = new RemotingDesc();
        remotingDesc.setProtocol(Protocols.IN_JVM.getCode());
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ProxyUtils.getInterfaces(classType);
        for(Class<?> interClass : interfaceClasses){
            if(interClass.isAnnotationPresent(LocalTCC.class)){
                remotingDesc.setInterfaceClassName(interClass.getName());
                remotingDesc.setInterfaceClass(interClass);
                remotingDesc.setTargetBean(bean);
                return remotingDesc;
            }
        }
        throw new FrameworkException("Couldn't parser any Remoting info");
    }

    @Override
    public Protocols getProtocol() {
        return Protocols.IN_JVM;
    }
}
