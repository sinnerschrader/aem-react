package com.sinnerschrader.aem.react.json;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceResolverHelperFactory {

	private final static String REQUEST_KEY = "com.sinnerschrader.aem.react.json.ResourceResolver.REQUEST_KEY";
	private static Pattern manglePattern = Pattern.compile("([^/]+):([^/]+)");

	public static ResourceResolverHelper create(SlingHttpServletRequest request, boolean mangleNameSpaces) {
		final Resource resource = request.getResource();
		final String resourcePath = resource.getPath();

		ResourceResolver resourceResolver = request.getResourceResolver();
		String mappedPath = createMappedPath(request, resourcePath);
		String mangledResourcePath = mangleNameSpaces ? mangleNamespaces(resourcePath) : resourcePath;
		int prefixLength = mangledResourcePath.indexOf(mappedPath);
		final ResourceResolverHelper resolver;
		if (prefixLength >= 0) {
			resolver = new ResourceResolverHelper(mangledResourcePath.substring(0, prefixLength), resourceResolver);
		} else {
			resolver = new ResourceResolverHelper("", resourceResolver);
		}

		return resolver;

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
