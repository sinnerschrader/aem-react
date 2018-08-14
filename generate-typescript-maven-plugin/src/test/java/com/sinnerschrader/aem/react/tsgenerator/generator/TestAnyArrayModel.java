package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.sinnerschrader.aem.reactapi.typescript.ExportTs;
import com.sinnerschrader.aem.reactapi.typescript.TsElement;

import lombok.Getter;

@Getter
@ExportTs
public class TestAnyArrayModel {
	@TsElement("any")
	private Object[] models;
}
