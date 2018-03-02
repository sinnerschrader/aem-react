package com.sinnerschrader.aem.react.api;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.granite.xss.XSSAPI;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class CqxTest {

	@Mock
	private Sling sling;

	@Mock
	private OsgiServiceFinder finder;

	@Mock
	private ModelFactory modelFactory;

	@Mock
	private XSSAPI xssApi;

	@Mock
	private JsProxy jsProxy;

	@Mock
	private ObjectMapper mapper;

	@Test
	public void testGetOsgiService() {
		String name = "dd";
		Mockito.when(finder.get(name, mapper)).thenReturn(jsProxy);
		Cqx cqx = createCqx();
		JsProxy p = cqx.getOsgiService(name);
		Assert.assertNotNull(p);
	}

	@Test
	public void testGetRequestModel() {
		String name = "dd";
		String path = "xx";
		Mockito.when(modelFactory.createRequestModel(path, name)).thenReturn(jsProxy);
		Cqx cqx = createCqx();
		JsProxy p = cqx.getRequestModel(path, name);
		Assert.assertNotNull(p);
	}

	@Test
	public void testGetResourceModel() {
		String name = "dd";
		String path = "xx";
		Mockito.when(modelFactory.createResourceModel(path, name)).thenReturn(jsProxy);
		Cqx cqx = createCqx();
		JsProxy p = cqx.getResourceModel(path, name);
		Assert.assertNotNull(p);
	}

	@Test
	public void testGetSling() {
		Cqx cqx = createCqx();
		Sling asling = cqx.getSling();
		Assert.assertEquals(sling, asling);
	}

	@Test
	public void testGetXssApi() {
		Cqx cqx = createCqx();
		XSSAPI api = cqx.getXssApi();
		Assert.assertEquals(xssApi, api);
	}

	private Cqx createCqx() {
		return new Cqx(sling, finder, modelFactory, xssApi, mapper);
	}

}
