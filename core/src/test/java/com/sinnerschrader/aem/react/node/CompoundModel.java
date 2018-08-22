package com.sinnerschrader.aem.react.node;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompoundModel {

	private String headline;
	public String getHeadline() {
		return headline;
	}
	public void setHeadline(String headline) {
		this.headline = headline;
	}
	public void setTestModel(TestModel testModel) {
		this.testModel = testModel;
	}
	private TestModel testModel;

	@JsonProperty(":items")
	public Map<String,Object> getExportedItems() {
		return new HashMap<String,Object>() {
			{
				put("test", testModel);
			}
		};
	}

}
