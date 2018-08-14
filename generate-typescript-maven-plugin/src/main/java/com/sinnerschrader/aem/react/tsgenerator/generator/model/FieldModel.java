package com.sinnerschrader.aem.react.tsgenerator.generator.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static java.util.Objects.requireNonNull;

@Getter
@ToString
@EqualsAndHashCode
public class FieldModel implements Comparable<FieldModel> {

	private final String name;
	private final String[] types;

	public FieldModel(String name, String... types) {
		this.name = requireNonNull(name);
		this.types = requireNonNull(types);
	}

	@Override
	public int compareTo(FieldModel o) {
		return name.compareTo(o.name);
	}
}
