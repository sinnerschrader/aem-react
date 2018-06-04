package com.sinnerschrader.aem.react.tsgenerator.descriptor;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TypeDescriptor
{
	public static final String NUMBER = "number";

	public static final String BOOL = "boolean";

	public static final String STRING = "string";

	public static final String ANY = "any";

	public static final String ARRAY = "TYPE[]";

	public static final String ARRAY_OF_ARRAY = "TYPE[][]";

	public static final String ANY_ARRAY = "any[]";

	public static final String MAP = "{}";

	public static final String NULL = "null";

	private String type;

	private boolean map;

	private boolean extern;

	private boolean array;

	private String path;



}
