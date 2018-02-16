package com.sinnerschrader.aem.react.mapping;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceResolverHelper {

	private String prefix;
	private ResourceResolver delegate;


	public Resource resolve(HttpServletRequest arg0, String arg1) {
		return delegate.resolve(arg0, resolveInternally(arg1));
	}

	public Resource resolve(String arg0) {
		return delegate.resolve(resolveInternally(arg0));
	}

	public Resource resolve(HttpServletRequest arg0) {
		return delegate.resolve(arg0);
	}

	public ResourceResolverHelper(String prefix, ResourceResolver delegate) {
		super();
		this.prefix = prefix;
		this.delegate = delegate;
	}

	public String resolveInternally(String uriPathOrUrl) {
		String uriPath;
		try {
			uriPath = new URL(uriPathOrUrl).getPath();
		} catch (Exception e) {
			uriPath = uriPathOrUrl;
		}
		if (uriPath.startsWith(this.prefix)) {
			return uriPath;
		}
		return this.prefix + uriPath;
	}

}
