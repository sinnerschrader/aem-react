package com.sinnerschrader.aem.react.tsgenerator.maven;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.plugins.annotations.Parameter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TsElementDefault {

	@Parameter(required = true)
	private String className;

	private String name;

	@Parameter(required = true)
	private String path;
}
