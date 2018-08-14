package com.sinnerschrader.aem.react.tsgenerator.generator;

import java.util.List;

import com.sinnerschrader.aem.reactapi.typescript.ExportTs;
import com.sinnerschrader.aem.reactapi.typescript.TsElement;

import lombok.Getter;

@Getter
@ExportTs
public class TestTsListModel {

	@TsElement("ts/Element")
	private List<TestModel> models;
}
