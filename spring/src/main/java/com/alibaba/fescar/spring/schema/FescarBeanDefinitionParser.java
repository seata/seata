package com.alibaba.fescar.spring.schema;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;

/**
 * 
* @ClassName: FescarBeanDefinitionParser 
* @Description: TODO(这里用一句话描述这个类的作用) 
* @author Cauchy
* @date 2019年3月1日 下午4:06:43 
*
 */
public class FescarBeanDefinitionParser implements BeanDefinitionParser {

	private static final String APPID="appId";
	
	private static final String TX_SERVICE_GROUP = "txServiceGroup";
	
	private static final String MODE = "mode";
	
	private static final String FAILURE_HANDLER = "failureHandler";
	
	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {

		RootBeanDefinition bd = new RootBeanDefinition();
		
		//默认注入扫描器;
		bd.setBeanClass(GlobalTransactionScanner.class);
		// 不允许lazy init
		bd.setLazyInit(false);

		// 如果没有id则按照规则生成一个id,注册id到context中
		String id = element.getAttribute("id");
		if (StringUtils.isEmpty(id)) {
			id = GlobalTransactionScanner.class.getName();
		}
		if (id != null && id.length() > 0) {
			if (parserContext.getRegistry().containsBeanDefinition(id)) {
				throw new IllegalStateException("Duplicate spring bean id " + id);
			}
			parserContext.getRegistry().registerBeanDefinition(id, bd);
		}
		
		String failHandler = element.getAttribute(FAILURE_HANDLER);
		if (StringUtils.isNotEmpty(failHandler))
		{
			if (parserContext.getRegistry().containsBeanDefinition(failHandler)) {
                BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(failHandler);
                if (!refBean.isSingleton()) {
                    throw new IllegalStateException("The exported service ref " + failHandler + " must be singleton! Please set the " + failHandler
                            + " bean scope to singleton, eg: <bean id=\"" + failHandler + "\" scope=\"singleton\" ...>");
                }
            }
		}
		
		bd.setConstructorArgumentValues(buildConstructorArgument(element));
		
		managerTransationConfig(element);
		
		return bd;
	}
	
	private void managerTransationConfig(Element element)
	{
		NodeList nodeList = element.getChildNodes();
		
		int len = nodeList.getLength();
		
		for (int i=0;i<len;i++)
		{
			Node node = nodeList.item(i);
			
			if (node instanceof Element)
			{
				Element nodeEle = (Element)node;
				TransationConfigBean configBean = new TransationConfigBean();
				
				String name = nodeEle.getAttribute("name");
				configBean.setName(name);
				
				int timeout = Integer.valueOf(nodeEle.getAttribute("timeout"));
				configBean.setTimeout(timeout);
				
				String ref = nodeEle.getAttribute("ref");
				configBean.setRef(ref);
				
				String method = nodeEle.getAttribute("method");
				configBean.setMethod(method);
				
				String scanPakeage = nodeEle.getAttribute("scan-pakeage");
				configBean.setScanPakeage(scanPakeage);
				
				TransationConfigManager.getInstance().putConfig(configBean);
			}
		}
	}
	
	private ConstructorArgumentValues buildConstructorArgument(Element element)
	{
		ConstructorArgumentValues constructor = new ConstructorArgumentValues();
		constructor.addIndexedArgumentValue(0, getProperty(element,APPID,val->val));
		constructor.addIndexedArgumentValue(1, getProperty(element,TX_SERVICE_GROUP,val->val));
		constructor.addIndexedArgumentValue(2, getProperty(element,MODE,val->Integer.valueOf(val)));
		constructor.addIndexedArgumentValue(3, new RuntimeBeanReference((String)getProperty(element,FAILURE_HANDLER,val->val)));
		return constructor;
	}
	
	private Object getProperty(Element ele,String key,GetterVal getterOperation)
	{
		String val = ele.getAttribute(key);
		
		if (StringUtils.isNotEmpty(val))
		{
			return getterOperation.doGet(val);
		}
		
		return null;
	}
	
	/**
	 * 
	 * @author Cauchy
	 *
	 */
	@FunctionalInterface
	private interface GetterVal
	{
		Object doGet(String val);
	}

}
