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
package io.seata.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * load config file from basedir, basedir can be set by System.Property
 * @author wuzq
 * @date May 22, 2019
 */
public class SeataConfigFileClassLoaderProxy extends ClassLoader{
	
	private String baseDir;
	private ClassLoader realClassLoader;
	
	public SeataConfigFileClassLoaderProxy(ClassLoader realClassLoader, String baseDir) {
		super();
		this.realClassLoader = realClassLoader;
		this.baseDir = baseDir;
	}
	
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		String extendResourceName = name;
		if(baseDir != null) {
			extendResourceName = baseDir + File.separator + name;
		}
		final List<URL> urls = new ArrayList<>();
		//load extend resource first
		File yytResourceFile = new File(extendResourceName);
		if(yytResourceFile.exists()) {
			URL url = yytResourceFile.toURI().toURL();
			urls.add(url);
		}
		//load resource from jar
		if(realClassLoader != null) {
			Enumeration<URL> oldResources = realClassLoader.getResources(name);
			//merge resources
			while(oldResources.hasMoreElements()) {
				urls.add(oldResources.nextElement());
			}
		}
		return new Enumeration<URL>() {
			Iterator<URL> iterator = urls.iterator();

			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			public URL nextElement() {
				return iterator.next();
			}
		};
	}
	
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if(realClassLoader != null) {
			return realClassLoader.loadClass(name);
		}
		return super.loadClass(name);
	}
	
	public java.io.InputStream getResourceAsStream(String name) {
		if(realClassLoader != null) {
			return realClassLoader.getResourceAsStream(name);
		}
		return super.getResourceAsStream(name);
	};

}
