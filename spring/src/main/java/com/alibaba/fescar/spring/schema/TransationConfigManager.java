package com.alibaba.fescar.spring.schema;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.fescar.spring.annotation.GlobalTransactional;

public class TransationConfigManager {

	private Map<String,TransationConfigBean> beanNameConfig = new HashMap<>();

	private Map<String,String> beanNameAndMethodMap = new HashMap<>();
	
	private Map<String,TransationConfigBean> scanPakageMap = new HashMap<>();
	
	private static final TransationConfigManager INSTANCE = new TransationConfigManager();
	
	public void putConfig(TransationConfigBean configBean)
	{
		if (null != configBean.getRef())
		{
			beanNameConfig.put(configBean.getRef(), configBean);
		}
		
		if (null != configBean.getScanPakeage())
		{
			scanPakageMap.put(configBean.getScanPakeage()+"#"+configBean.getMethod(), configBean);
		}
	}
	
	public boolean isContainBeanName(String beanName)
	{
		return beanNameConfig.containsKey(beanName);
	}
	
	public void putBeanNameAndMethodMapping(String beanName,Method method)
	{
		if (null != method.getAnnotation(GlobalTransactional.class))
		{
			//构造配置,与配置文件形成统一的入口;
			GlobalTransactional gtn = method.getAnnotation(GlobalTransactional.class);
			TransationConfigBean configBean = new TransationConfigBean();
			configBean.setName(gtn.name());
			configBean.setTimeout(gtn.timeoutMills());
			configBean.setRef(beanName);
			configBean.setMethod(method.getName());
			putConfig(configBean);
		}
		beanNameAndMethodMap.put(method.getDeclaringClass().getName()+ "#" + method.getName(), beanName);
	}
	
	public TransationConfigBean getConfig(String key)
	{
		String beanName = beanNameAndMethodMap.get(key.trim());
		
		if (null != beanName)
		{
			return beanNameConfig.get(beanName);
		}
		
		return null;
	}
	
	public boolean isFixedMethod(String beanName,Method method)
	{
		if (null != method.getAnnotation(GlobalTransactional.class))
		{
			return true;
		}
		
		return checkMethod(beanName,method);
	}
	
	private boolean checkMethod(String beanName,Method method)
	{
		if (isContainBeanName(beanName))
		{
			TransationConfigBean configBean = beanNameConfig.get(beanName);
			return Pattern.matches(configBean.getMethod(),method.getName());
		}
		
		String clazzName = method.getDeclaringClass().getName();

		for (Map.Entry<String, TransationConfigBean> entry : scanPakageMap.entrySet())
		{
			String scanPakeagePrefix = entry.getKey().split("#")[0];
			if (clazzName.startsWith(scanPakeagePrefix))
			{
				TransationConfigBean configBean = entry.getValue();
				boolean ret = Pattern.matches(configBean.getMethod(),method.getName());
				if (ret)
				{
					beanNameConfig.put(beanName, configBean);
					return ret;
				}
			}
		}
		
		return false;
	}
	
	private TransationConfigManager()
	{
		
	}
	
	public static TransationConfigManager getInstance()
	{
		return INSTANCE;
	}
	
	
}
