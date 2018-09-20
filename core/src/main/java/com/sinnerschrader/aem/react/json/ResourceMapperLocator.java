package com.sinnerschrader.aem.react.json;

import org.apache.sling.api.SlingHttpServletRequest;

public class ResourceMapperLocator {

	public static ThreadLocal<ResourceMapper> mapperHolder = new ThreadLocal<>();

	public static ResourceMapper getInstance() {
		return mapperHolder.get();
	}

	public static ResourceMapper setInstance(ResourceMapper mapper) {
		ResourceMapper oldVal = getInstance();
		mapperHolder.set(mapper);
		return oldVal;
	}

	public static ResourceMapper setInstance(SlingHttpServletRequest request) {
		return setInstance(new ResourceMapper(request));
	}

	public static void clearInstance() {
		mapperHolder.remove();
	}

}
