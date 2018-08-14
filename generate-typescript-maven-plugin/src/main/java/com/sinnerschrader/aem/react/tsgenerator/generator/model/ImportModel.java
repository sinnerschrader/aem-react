package com.sinnerschrader.aem.react.tsgenerator.generator.model;

import lombok.Getter;

@Getter
public class ImportModel implements Comparable<ImportModel> {

	private final String name;
	private final String path;

	public ImportModel(String name, String path) {
		this.name = name;
		this.path = path;
	}

	@Override
	public int compareTo(ImportModel o) {
		return name.compareTo(o.name);
	}
}
