package com.sinnerschrader.aem.react.cache;

import java.util.HashMap;
import java.util.Map;

public class ModelCollector {

	private Map<String, Object> models;

	public void addRequestModel(String path, Object model) {
		if (model == null) {
			return;
		}
		if (models == null) {
			models = new HashMap();
		}
		this.models.put(path, model);
	}

	public Object getModel(String path, String name) {
		if (models != null) {
			Object model = models.get(path);
			if (model != null && model.getClass().getName().equals(name)) {
				return model;
			}
		}
		return null;
	}

}
