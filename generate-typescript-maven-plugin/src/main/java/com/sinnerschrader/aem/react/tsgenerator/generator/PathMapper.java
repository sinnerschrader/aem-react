package com.sinnerschrader.aem.react.tsgenerator.generator;

public class PathMapper implements IPathMapper {

	private final String currents[];

	public PathMapper(String current) {
		currents = current.split("\\.");
	}

	public String apply(final String name) {
		String[] names = name.split("\\.");
		int idx = 0;
		while (idx < names.length && idx < currents.length && currents[idx].equals(names[idx])) {
			idx++;
		}
		int downCount = currents.length - idx - 1;
		StringBuilder builder = new StringBuilder();
		if (downCount > 0) {
			for (int i = 0; i < downCount; i++) {
				builder.append("../");
			}
		} else {
			builder.append("./");
		}
		for (int i = idx; i < names.length - 1; i++) {
			builder.append(names[i]);
			builder.append("/");
		}
		builder.append(names[names.length - 1]);
		return builder.toString();
	}

}
