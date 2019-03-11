package com.alibaba.fescar.spring.schema;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 
* @ClassName: FescarNameSpaceHandler 
* @Description: TODO(这里用一句话描述这个类的作用) 
* @author Cauchy
* @date 2019年3月1日 下午4:09:27 
*
 */
public class FescarNameSpaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		//解析<fescar:txs>标签;
		registerBeanDefinitionParser("txs", new FescarBeanDefinitionParser());
	}

}
