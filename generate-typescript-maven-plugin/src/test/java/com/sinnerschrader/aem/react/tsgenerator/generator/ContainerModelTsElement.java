package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ContainerExporter;
import com.sinnerschrader.aem.reactapi.typescript.ExportTs;
import com.sinnerschrader.aem.reactapi.typescript.TsElement;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@ExportTs
@Getter
public class ContainerModelTsElement implements @TsElement("infrastructure/ContainerExporter") ContainerExporter {

	public static class ComponentModel1 implements ComponentExporter {

		@Nonnull
		@Override
		public String getExportedType() {
			return "/components/model1";
		}
	}

	public static class ComponentModel2 implements ComponentExporter {

		@Nonnull
		@Override
		public String getExportedType() {
			return "/components/model2";
		}
	}


	private String stringField;
	private boolean booleanField;

	@Nonnull
	@Override
	public Map<String, ? extends ComponentExporter> getExportedItems() {
		HashMap<String, ComponentExporter> map = new HashMap<>();
		map.put("model1", new ComponentModel1());
		map.put("model2", new ComponentModel2());
		return map;
	}

	@Nonnull
	@Override
	public String[] getExportedItemsOrder() {
		return new String[] { "model1", "model2" };
	}

	@Nonnull
	@Override
	public String getExportedType() {
		return "/container/test";
	}
}
