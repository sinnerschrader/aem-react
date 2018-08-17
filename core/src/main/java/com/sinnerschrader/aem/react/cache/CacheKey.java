package com.sinnerschrader.aem.react.cache;

import java.util.List;

public class CacheKey {
	public CacheKey(String path, String resourceType, String wcmmode, boolean renderAsJson, List<String> selectors) {
		super();
		this.path = path;
		this.resourceType = resourceType;
		this.wcmmode = wcmmode;
		this.renderAsJson = renderAsJson;
		this.selectors = selectors;
	}

	private final String path;
	private final String resourceType;
	private final String wcmmode;
	private final boolean renderAsJson;
	private final List<String> selectors;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + (renderAsJson ? 1231 : 1237);
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		result = prime * result + ((selectors == null) ? 0 : selectors.hashCode());
		result = prime * result + ((wcmmode == null) ? 0 : wcmmode.hashCode());
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
		CacheKey other = (CacheKey) obj;
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		if (renderAsJson != other.renderAsJson) {
			return false;
		}
		if (resourceType == null) {
			if (other.resourceType != null) {
				return false;
			}
		} else if (!resourceType.equals(other.resourceType)) {
			return false;
		}

		if (selectors == null) {
			if (other.selectors != null) {
				return false;
			}
		} else if (!selectors.equals(other.selectors)) {
			return false;
		}
		if (wcmmode == null) {
			if (other.wcmmode != null) {
				return false;
			}
		} else if (!wcmmode.equals(other.wcmmode)) {
			return false;
		}
		return true;
	}

}
