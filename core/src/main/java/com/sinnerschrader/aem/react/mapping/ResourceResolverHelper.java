package com.sinnerschrader.aem.react.mapping;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceResolverHelper extends ResourceResolverWrapper{

	private String prefix;
	private ResourceResolver delegate;


	public Resource resolve(HttpServletRequest request, String path)  {

			return delegate.resolve(request, resolveInternally(path));
	}

	public Resource resolve(String arg0) {
		return delegate.resolve(resolveInternally(arg0));
	}

	public Resource resolve(HttpServletRequest arg0) {
		return delegate.resolve(arg0);
	}

	public ResourceResolverHelper(String prefix, ResourceResolver delegate) {
		super(delegate);
		this.prefix = prefix;
		this.delegate = delegate;
	}

	public String resolveInternally(String uriPathOrUrl) {
		try {
			uriPathOrUrl = URLDecoder.decode(uriPathOrUrl,"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// ignore
		}
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
