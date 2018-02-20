package com.sinnerschrader.aem.react.mapping;

import java.net.MalformedURLException;
import java.net.URL;

public class ResourceResolverUtils {
	public static String getUriPath(String urlOrPath) {
		if (urlOrPath.startsWith("http")) {
			try {
				URL url = new URL(urlOrPath);
				return url.getPath();
			} catch (MalformedURLException e) {
				return urlOrPath;
			}
		}
		return urlOrPath;
	}
}
