package com.sinnerschrader.aem.react.tsgenerator.descriptor;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PropertyDescriptor {
	private TypeDescriptor type;

	private String name;

	private boolean isNotNullable;

}
