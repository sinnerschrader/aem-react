package com.sinnerschrader.aem.react.tsgenerator.generator;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

public class TsPathMapper implements IPathMapper {

	// TODO check how to avoid getting called with typescript types (or combinations
	// of it like string[] or Array<string>)
	private static final Set<String> TS_TYPES = new HashSet<>(
			asList("any", "void", "object", "boolean", "number", "string"));

	private final String className;
	private final String basePackage;
	private final String relativePath;

	public TsPathMapper(String className, String basePackage, String relativePath) {
		this.className = className;
		this.basePackage = basePackage;
		this.relativePath = ensureTrailingSlash(relativePath);
	}

	@Override
	public String apply(String name) {
		if (TS_TYPES.contains(name)) {
			return name;
		}

		final String shortenedClassName;
		if (className.startsWith(basePackage)) {
			shortenedClassName = className.substring(basePackage.length() + 1);
		} else {
			shortenedClassName = className;
		}
		if (name.startsWith("/")) {
			return name.substring(1);
		}
		int depth = countPackageDepth(shortenedClassName);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append("../");
		}
		if (relativePath != null) {
			sb.append(relativePath);
		}
		return sb.append(name).toString();
	}

	static String ensureTrailingSlash(String path) {
		if (path == null || path.length() == 0 || path.endsWith("/")) {
			return path;
		}
		return path + "/";
	}

	static int countPackageDepth(String className) {
		final int len = className.length();
		if (len == 0) {
			return 0;
		}
		int depth = 0;
		for (int i = 0; i < len; i++) {
			char c = className.charAt(i);
			if (c == '.') {
				depth++;
			}
		}
		return depth;
	}
}
