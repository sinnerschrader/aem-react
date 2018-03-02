package com.sinnerschrader.aem.react.integration;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = SlingHttpServletRequest.class)
public class TextProps {

	public String getContent() {
		return "RequestModel";
	}

}
