package com.alibaba.fescar.spring.schema;

public class ServiceImpl implements Service {

	@Override
	public String pay(String json) {
		
		System.out.println("input = " + json);
		
		return "ok";
	}

	@Override
	public String payService(String json) {
		System.out.println("input = " + json);
		
		return "ok";
	}

}
