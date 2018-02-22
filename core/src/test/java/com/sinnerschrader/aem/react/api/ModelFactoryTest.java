package com.sinnerschrader.aem.react.api;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.impl.injectors.SlingObjectInjector;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class ModelFactoryTest {

	@Rule
	public SlingContext context = new SlingContext();

	public static class TestModel {

		public TestModel(SlingHttpServletRequest request) {
			this.request = request;
		}

		public TestModel(Resource resource) {
			this.resource = resource;
		}

		private SlingHttpServletRequest request;

		private Resource resource;

		public int getCount() {
			return 1;
		}

		public String getResourcePath() {
			return resource.getPath();
		}

		public String getRequestResourcePath() {
			return request.getResource().getPath();
		}
	}

	@Mock
	private SlingHttpServletRequest request;
	@Mock
	private org.apache.sling.models.factory.ModelFactory modelFactory;
	@Mock
	private AdapterManager adapterManager;
	@Mock
	private ObjectMapper mapper;
	@Mock
	private ResourceResolver resourceResolver;

	@Test
	public void testRequestModel() throws Exception {
		context.registerService(Injector.class, new SlingObjectInjector());

		Resource resource = context.create().resource("/test");
		context.currentResource(resource);

		context.addModelsForClasses("com.sinnerschrader.aem.react.api");
		Mockito.when(modelFactory.createModel(Mockito.any(SlingHttpServletRequest.class), Mockito.any()))
				.thenAnswer((InvocationOnMock invoke) -> {
					return ((Class) invoke.getArguments()[1])
							.getConstructor(new Class[] { SlingHttpServletRequest.class })
							.newInstance(invoke.getArguments()[0]);
				});
		ModelFactory factory = new ModelFactory(getClass().getClassLoader(), context.request(), modelFactory,
				adapterManager, new ObjectMapper(), context.resourceResolver());

		JsProxy model = factory.createRequestModel("/test", TestModel.class.getName());

		Assert.assertEquals("1", model.get("count"));
		Assert.assertEquals("\"/test\"", model.get("requestResourcePath"));
	}

	@Test
	public void testResourceModel() throws Exception {
		context.create().resource("/test");

		context.addModelsForClasses("com.sinnerschrader.aem.react.api");
		Mockito.when(modelFactory.createModel(Mockito.any(Resource.class), Mockito.any()))
				.thenAnswer((InvocationOnMock invoke) -> {
					return ((Class) invoke.getArguments()[1]).getConstructor(new Class[] { Resource.class })
							.newInstance(invoke.getArguments()[0]);
				});
		ModelFactory factory = new ModelFactory(getClass().getClassLoader(), context.request(), modelFactory,
				adapterManager, new ObjectMapper(), context.resourceResolver());

		JsProxy model = factory.createResourceModel("/test", TestModel.class.getName());

		Assert.assertEquals("1", model.get("count"));
		Assert.assertEquals("\"/test\"", model.get("resourcePath"));
	}

}
