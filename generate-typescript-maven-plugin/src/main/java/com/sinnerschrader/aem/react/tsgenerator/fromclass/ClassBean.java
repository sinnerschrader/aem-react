package com.sinnerschrader.aem.react.tsgenerator.fromclass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.sinnerschrader.aem.react.tsgenerator.generator.IPathMapper;
import com.sinnerschrader.aem.react.tsgenerator.generator.TsPathMapper;

import lombok.Getter;

public class ClassBean {

	private static final Pattern DOT_PATTERN = Pattern.compile("([^\\.]*)$");
	private static final Pattern SLASH_PATTERN = Pattern.compile("([^/]*)$");

	private Class<?> type;
	@Getter
	private final String simpleName;
	@Getter
	private final String name;
	@Getter
	private final boolean extern;
	@Getter
	private final IPathMapper pathMapper;

	public ClassBean(Class type) {
		this(type, null);
	}

	public ClassBean(Class type, IPathMapper pathMapper) {
		this.name = type.getName();
		this.simpleName = type.getSimpleName();
		this.type = type;
		this.extern = true;
		this.pathMapper = pathMapper;
	}

	public ClassBean(String name) {
		this(name, null);
	}

	public ClassBean(String name, IPathMapper pathMapper) {
		this.name = name;
		this.simpleName = extractSimpleName(name);
		this.extern = !simpleName.equals(name);
		this.pathMapper = pathMapper;
	}

	public ClassBean(String value, String name, TsPathMapper tsPathMapper) {
		this.name = value;
		this.simpleName = (StringUtils.isEmpty(name)) ? extractSimpleName(value) : name;
		this.extern = !simpleName.equals(value);
		this.pathMapper = tsPathMapper;
	}

	private static String extractSimpleName(String name) {
		final Matcher matcher;
		if (name.indexOf('.') >= 0) {
			matcher = DOT_PATTERN.matcher(name);
		} else if (name.indexOf('/') >= 0) {
			matcher = SLASH_PATTERN.matcher(name);
		} else {
			return name;
		}
		if (matcher.find()) {
			return matcher.group(0);
		}
		return name;
	}

	public boolean isNumber() {
		return this.type != null && Number.class.isAssignableFrom(this.type);
	}

	public boolean isBoolean() {
		return this.type != null && Boolean.class.isAssignableFrom(this.type);
	}

	public boolean isString() {
		return this.type != null && String.class.isAssignableFrom(this.type);
	}

	public boolean isExtern() {
		return extern;
	}
}
