package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.adobe.cq.export.json.ComponentExporter;
import com.sinnerschrader.aem.reactapi.typescript.TsElement;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@Getter
public class SubContainerModelTsElement implements @TsElement("spa/SubContainerExporter") SubContainerExporter {

	public static class ComponentModel implements ComponentExporter {

		@Nonnull
		@Override
		public String getExportedType() {
			return "/components/model";
		}
	}

	@Nonnull
	@Override
	public Map<String, ? extends ComponentExporter> getExportedItems() {
		HashMap<String, ComponentExporter> map = new HashMap<>();
		map.put("model1", new ComponentModel());
		map.put("model2", new ComponentModel());
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

	@Override
	public String getStringField() {
		return "foo";
	}
}
