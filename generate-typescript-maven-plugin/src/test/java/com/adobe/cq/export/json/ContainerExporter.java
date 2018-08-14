package com.adobe.cq.export.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import java.util.Map;

public interface ContainerExporter extends ComponentExporter {

	@Nonnull
	@JsonProperty(":items")
	Map<String, ? extends ComponentExporter> getExportedItems();

	@Nonnull
	@JsonProperty(":itemsOrder")
	String[] getExportedItemsOrder();
}
