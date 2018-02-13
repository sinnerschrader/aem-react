package com.sinnerschrader.aem.react.json;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ObjectMapperFactory {

	public ObjectMapper create(String includePattern, String excludePattern) {
		ObjectMapper objectMapper = new ObjectMapper();
		if (StringUtils.isNoneEmpty(includePattern)) {
			SimpleModule module = new SimpleModule();
			module.addSerializer(String.class, new StringSerializer(includePattern, excludePattern));
			objectMapper.registerModule(module);
		}
		return objectMapper;

	}

}
