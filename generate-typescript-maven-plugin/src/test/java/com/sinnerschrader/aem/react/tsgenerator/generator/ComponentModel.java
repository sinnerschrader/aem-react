package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.adobe.cq.export.json.ComponentExporter;
import com.sinnerschrader.aem.reactapi.typescript.ExportTs;
import lombok.Getter;

import javax.annotation.Nonnull;

@ExportTs
public class ComponentModel implements ComponentExporter {

	@Getter
	private String stringField;

	@Nonnull
	@Override
	public String getExportedType() {
		return "/components/test";
	}
}
