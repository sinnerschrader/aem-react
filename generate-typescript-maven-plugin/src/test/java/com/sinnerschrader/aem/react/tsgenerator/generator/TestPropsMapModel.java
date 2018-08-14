package com.sinnerschrader.aem.react.tsgenerator.generator;

import java.util.Map;

import com.sinnerschrader.aem.reactapi.typescript.ExportTs;
import com.sinnerschrader.aem.reactapi.typescript.TsElement;

import lombok.Getter;

@Getter
@ExportTs
public class TestPropsMapModel {
	@TsElement("de.Props")
	private Map<String, Object> models;
}
