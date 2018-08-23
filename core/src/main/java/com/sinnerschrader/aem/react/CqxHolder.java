package com.sinnerschrader.aem.react;

import java.io.IOException;

import com.adobe.granite.xss.XSSAPI;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.api.JsProxy;
import com.sinnerschrader.aem.react.api.Sling;

public class CqxHolder {

	private Cqx cqx;

	public JsProxy getOsgiService(String name) {
		return cqx.getOsgiService(name);
	}

	/**
	 * get a request sling model
	 *
	 * @param name
	 *            fully qualified class name
	 * @return
	 */
	public JsProxy getRequestModel(String path, String name) {
		return cqx.getRequestModel(path, name);
	}

	public String getModel(String path) throws JsonGenerationException, JsonMappingException, IOException {
		return cqx.getModel(path);
	}

	/**
	 * get a resource sling model
	 *
	 * @param name
	 *            fully qualified class name
	 * @return
	 */
	public JsProxy getResourceModel(String path, String name) {
		return cqx.getResourceModel(path, name);
	}

	/**
	 * get access to resource via the sling objects
	 *
	 * @return
	 */
	public Sling getSling() {
		return cqx.getSling();
	}

	public XSSAPI getXssApi() {
		return cqx.getXssApi();
	}

	public void init(Cqx cqx) {
		this.cqx = cqx;
	}
}