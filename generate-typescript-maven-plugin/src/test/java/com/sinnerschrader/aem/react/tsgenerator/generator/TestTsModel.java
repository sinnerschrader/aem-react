package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.sinnerschrader.aem.reactapi.typescript.ExportTs;
import com.sinnerschrader.aem.reactapi.typescript.TsElement;

import lombok.Getter;

@Getter
@ExportTs
public class TestTsModel {

	@TsElement(value = "/ts/Element", name = "X")
	private Object model;
}
