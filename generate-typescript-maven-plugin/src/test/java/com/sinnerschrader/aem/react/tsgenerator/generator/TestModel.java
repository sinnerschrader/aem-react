package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sinnerschrader.aem.reactapi.typescript.ExportTs;

@ExportTs
public class TestModel {
	private String value;

	public String getValue() {
		return value;
	}

	private boolean done;

	@JsonIgnore
	public boolean isDone() {
		return done;
	}

}
