/**
 * @(#)Service2Impl.java
 * 版 权：有棵树集团
 * @author 125C01063111
 * @version 1.0 2019年3月8日
 *
 * Copyright (C) 2000,2019 , TeamSun, Inc.
 */
package com.alibaba.fescar.spring.schema;

import com.alibaba.fescar.spring.annotation.GlobalTransactional;

/**   
* @Title: Service2Impl.java 
* @Description: TODO(用一句话描述该文件做什么) 
* @author 125C01063111
* @date 2019年3月8日 下午3:56:11 
* 修 改 人：
* 修改时间：
* 修改单号：
* 修改内容：
*/
public class Service2Impl implements Service
{
	
	@Override
	public String pay(String json)
	{
		return "ok + " + json;
	}
	
	@Override
	@GlobalTransactional(name="glTest",timeoutMills=40000)
	public String payService(String json)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}



/**
 * Revision history
 * -------------------------------------------------------------------------
 * 
 * Date Author Note
 * -------------------------------------------------------------------------
 * 2019年3月8日 125C01063111 创建版本
 */