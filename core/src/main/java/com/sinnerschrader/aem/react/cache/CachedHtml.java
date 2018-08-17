package com.sinnerschrader.aem.react.cache;

import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;

public class CachedHtml {

	private RenderResult renderResult;
	private String model;
	private Integer rootNo;

	public Integer getRootNo() {
		return rootNo;
	}

	public CachedHtml(String cacheableModel, RenderResult result, Integer rootNo) {
		this.renderResult = result;
		this.model = cacheableModel;
		this.rootNo=rootNo;
	}

	public boolean isModelUnchanged(String cacheableModel) {
		return model.equals(cacheableModel);
	}

	public RenderResult getRenderResult() {
		return renderResult;
	}

	public String getModel() {
		return model;
	}

}
