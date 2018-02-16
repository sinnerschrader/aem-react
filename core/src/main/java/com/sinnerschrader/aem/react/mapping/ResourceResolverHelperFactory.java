package com.sinnerschrader.aem.react.mapping;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceResolverHelperFactory {

	private static Pattern manglePattern = Pattern.compile("([^/]+):([^/]+)");

	public static ResourceResolverHelper createHelper(SlingHttpServletRequest request, boolean mangleNameSpaces) {
		final Resource resource = request.getResource();
		final String resourcePath = resource.getPath();

		ResourceResolver resourceResolver = request.getResourceResolver();
		String mappedPath = createMappedPath(request, resourcePath);
		String mangledResourcePath = mangleNameSpaces ? mangleNamespaces(resourcePath) : resourcePath;
		int prefixLength = mangledResourcePath.indexOf(mappedPath);
		if (prefixLength >= 0) {
			return new ResourceResolverHelper(mangledResourcePath.substring(0, prefixLength), resourceResolver);
		}
		return null;

	}

	public static ResourceResolver create(SlingHttpServletRequest request, boolean mangleNameSpaces) {
		final ResourceResolver resourceResolver = request.getResourceResolver();
		ResourceResolverHelper resolver = createHelper(request, mangleNameSpaces);
		if (resolver != null) {
			return (ResourceResolver) Proxy.newProxyInstance(ResourceResolverHelperFactory.class.getClassLoader(),
					new Class[] { ResourceResolver.class }, new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							if (method.getName().equals("resolve")) {
								return method.invoke(resolver, args);
							}
							return method.invoke(resourceResolver, args);
						}
					});
		}
		return resourceResolver;

	}

	public static String mangleNamespaces(String path) {
		Matcher matcher = manglePattern.matcher(path);
		StringBuffer builder = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(builder, "_" + matcher.group(1) + "_" + matcher.group(2));
		}
		matcher.appendTail(builder);
		return builder.toString();
	}

	private static String createMappedPath(SlingHttpServletRequest request, final String resourcePath) {
		String mappedPath = request.getResourceResolver().map(request, resourcePath);

		try {
			mappedPath = new URL(mappedPath).getPath();
		} catch (MalformedURLException e) {

		}
		return mappedPath;
	}

}
