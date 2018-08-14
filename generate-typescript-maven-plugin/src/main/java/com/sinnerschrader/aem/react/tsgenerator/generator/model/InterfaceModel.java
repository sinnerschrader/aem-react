package com.sinnerschrader.aem.react.tsgenerator.generator.model;

import java.util.SortedSet;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class InterfaceModel implements Comparable<InterfaceModel> {

	private SortedSet<FieldModel> fields;
	private String name;
	private String superClasses;
	private String fullSlingModelName;
	private SortedSet<ImportModel> imports;
	private UnionModel unionModel;
	private DiscriminatorModel discriminator;

	@Override
	public int compareTo(InterfaceModel o) {
		return fullSlingModelName.compareTo(o.fullSlingModelName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fullSlingModelName == null) ? 0 : fullSlingModelName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		InterfaceModel other = (InterfaceModel) obj;
		if (fullSlingModelName == null) {
			if (other.fullSlingModelName != null) {
				return false;
			}
		} else if (!fullSlingModelName.equals(other.fullSlingModelName)) {
			return false;
		}
		return true;
	}
}
