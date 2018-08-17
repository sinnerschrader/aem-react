package com.sinnerschrader.aem.react.api;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 *
 * This is the proxy that is wrapped around each sling model and osgi service
 * exposed to javascript. It provides a reflective API and the conversion to
 * JSON of the return values.
 *
 * @author stemey
 *
 */
public class JsProxy {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsProxy.class);

	private Object target;

	private Map<String, Method> methods = new HashMap<>();

	private ObjectWriter writer;

	public JsProxy(Object target, Class<?> clazz, ObjectMapper mapper) {
		super();
		this.writer = mapper.writerWithView(Object.class);
		this.target = target;
		for (Method m : clazz.getMethods()) {
			methods.put(m.getName(), m);
		}
	}

	/**
	 *
	 * @param name
	 *            the name of the method to invoke on the target object
	 * @param args
	 *            parameters to be passed to the target method
	 * @return
	 * @throws Exception
	 */
	public String invoke(String name, Object[] args) throws Exception {
		try {
			Method method = methods.get(name);
			Object returnValue = method.invoke(target, args);
			return writeValue(returnValue);
		} catch (Exception e) {
			LOGGER.error("cannot invoke proxied method " + name, e);
			throw e;
		}
	}

	/**
	 *
	 * @param name
	 *            the name of the property
	 * @return value of property as json string
	 * @throws Exception
	 */
	public String get(String name) throws Exception {
		try {
			Method method = methods.get("get" + StringUtils.capitalize(name));
			Object returnValue = method.invoke(target, new Object[0]);
			return writeValue(returnValue);
		} catch (Exception e) {
			LOGGER.error("cannot invoke proxied method " + name, e);
			throw e;
		}
	}

	private String writeValue(Object returnValue) throws IOException, JsonGenerationException, JsonMappingException {
		StringWriter stringWriter = new StringWriter();
		writer.writeValue(stringWriter, returnValue);
		return stringWriter.toString();
	}

	/**
	 *
	 * @return object as json string
	 * @throws Exception
	 */
	public String getObject() throws Exception {
		try {
			StringWriter stringWriter = new StringWriter();
			writer.writeValue(stringWriter, target);
			return stringWriter.toString();
		} catch (Exception e) {
			LOGGER.error("cannot serialize object", e);
			throw e;
		}
	}

}
