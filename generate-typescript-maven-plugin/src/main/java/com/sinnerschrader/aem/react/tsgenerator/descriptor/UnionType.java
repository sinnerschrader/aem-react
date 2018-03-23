package com.sinnerschrader.aem.react.tsgenerator.descriptor;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UnionType {

	private TypeDescriptor descriptor;

	private List<Discriminator> discriminators;

	private String field;

	public boolean isDirectBaseDescendant(Class<?> clazz) {
		return clazz.getSuperclass().getName().equals(descriptor.getType());
	}

}
