package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.sinnerschrader.aem.reactapi.typescript.Element;
import com.sinnerschrader.aem.reactapi.typescript.ExportTs;

import lombok.Getter;

@Getter
@ExportTs
public class TestArrayModel {
	@Element(TestModel.class)
	private TestModel[] models;
}
