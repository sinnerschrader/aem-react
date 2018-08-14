package com.sinnerschrader.aem.react.tsgenerator.descriptor;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class ClassDescriptor {

	private String name;

	private String fullJavaClassName;

	private Discriminator discriminator;

	private UnionType unionType;

	private TypeDescriptor superClass;

	@Singular
	private Map<String, PropertyDescriptor> properties;

	private final Map<String, EnumDescriptor> enums = new LinkedHashMap<>();

	@Singular
	private Map<String, ExtendedInterface> extendedInterfaces;
}
