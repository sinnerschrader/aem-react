package com.sinnerschrader.aem.react.json;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
		String pathPrefix = getPathPrefix(mangledResourcePath, mappedPath);
		if (pathPrefix.length() > 0) {
			return new ResourceResolverHelper(pathPrefix, resourceResolver);
		}
		return null;

	}

	public static String getPathPrefix(String aPath, String bPath) {
		String[] aSegments = aPath.split("/");
		String[] bSegments = bPath.split("/");
		if (aSegments.length > bSegments.length) {
			return createPrefix(aSegments, bSegments);
		} else if (bSegments.length > aSegments.length) {
			return createPrefix(bSegments, aSegments);
		} else {
			return "";
		}

	}

	private static String createPrefix(String[] aSegments, String[] bSegments) {
		return Arrays.asList(aSegments).subList(0, aSegments.length - bSegments.length + 1).stream()
				.collect(Collectors.joining("/"));
	}

	public static ResourceResolver create(SlingHttpServletRequest request, boolean mangleNameSpaces) {
		final ResourceResolver resourceResolver = request.getResourceResolver();
		ResourceResolverHelper resolver = createHelper(request, mangleNameSpaces);
		if (resolver != null) {
			return resolver;
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
