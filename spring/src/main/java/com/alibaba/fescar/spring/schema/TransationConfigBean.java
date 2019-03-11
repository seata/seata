package com.alibaba.fescar.spring.schema;

public class TransationConfigBean {

	private String name;
	
	private int timeout;
	
	private String ref;
	
	private String method;
	
	private String scanPakeage;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getScanPakeage() {
		return scanPakeage;
	}

	public void setScanPakeage(String scanPakeage) {
		this.scanPakeage = scanPakeage;
	}

	@Override
	public String toString()
	{
		return "TransationConfigBean [name=" + name + ", timeout=" + timeout + ", ref=" + ref + ", method=" + method
				+ ", scanPakeage=" + scanPakeage + "]";
	}
	
	
}
