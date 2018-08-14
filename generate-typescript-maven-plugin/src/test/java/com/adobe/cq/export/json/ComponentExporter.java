package com.adobe.cq.export.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public interface ComponentExporter {

	@Nonnull
	@JsonProperty(":type")
	String getExportedType();
}
