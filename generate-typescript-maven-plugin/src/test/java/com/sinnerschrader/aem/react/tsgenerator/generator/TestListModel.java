package com.sinnerschrader.aem.react.tsgenerator.generator;

import java.util.List;

import com.sinnerschrader.aem.reactapi.typescript.Element;
import com.sinnerschrader.aem.reactapi.typescript.ExportTs;

import lombok.Getter;

@Getter
@ExportTs
public class TestListModel {
	@Element(TestModel.class)
	private List<TestModel> models;
}
