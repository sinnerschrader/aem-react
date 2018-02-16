package com.sinnerschrader.aem.react.json;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

public class ReactSlingHttpServletRequestWrapper extends SlingHttpServletRequestWrapper {

	private ResourceResolver resolver;

	public ReactSlingHttpServletRequestWrapper(SlingHttpServletRequest wrappedRequest, ResourceResolver resolver) {
		super(wrappedRequest);
		this.resolver = resolver;
	}

	@Override
	public ResourceResolver getResourceResolver() {
		return resolver;
	}

}
