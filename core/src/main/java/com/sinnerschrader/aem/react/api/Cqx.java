package com.sinnerschrader.aem.react.api;

import java.io.IOException;

import com.adobe.granite.xss.XSSAPI;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * An instance of this class is made available to the js script engine.
 *
 * @author stemey
 *
 */
public class Cqx {

	private Sling sling;
	private OsgiServiceFinder finder;
	private ModelFactory modelFactory;
	private XSSAPI xssApi;
	private ObjectMapper mapper;

	public Cqx(Sling sling, OsgiServiceFinder finder, ModelFactory modelFactory, XSSAPI xssApi, ObjectMapper mapper) {
		super();
		this.sling = sling;
		this.finder = finder;
		this.modelFactory = modelFactory;
		this.xssApi = xssApi;
		this.mapper = mapper;
	}

	/**
	 * get an osgi service
	 *
	 * @param name
	 *            fully qualified class name
	 * @return
	 */
	public JsProxy getOsgiService(String name) {
		return finder.get(name, mapper);
	}

	/**
	 * get a request sling model
	 *
	 * @param name
	 *            fully qualified class name
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	public String getModel(String path) throws JsonGenerationException, JsonMappingException, IOException {
		return modelFactory.createModel(path);
	}


	/**
	 * get a request sling model
	 *
	 * @param name
	 *            fully qualified class name
	 * @return
	 */
	public JsProxy getRequestModel(String path, String name) {
		return modelFactory.createRequestModel(path, name);
	}

	/**
	 * get a resource sling model
	 *
	 * @param name
	 *            fully qualified class name
	 * @return
	 */
	public JsProxy getResourceModel(String path, String name) {
		return modelFactory.createResourceModel(path, name);
	}

	/**
	 * get access to resource via the sling objects
	 *
	 * @return
	 */
	public Sling getSling() {
		return sling;
	}

	public XSSAPI getXssApi() {
		return xssApi;
	}

}
