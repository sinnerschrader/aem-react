package com.sinnerschrader.aem.react.api;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.metrics.MetricsHelper;

/**
 *
 * This class adapts objects to a target class.
 *
 * @author stemey
 *
 */
public class ModelFactory {

	private static final class ReactSlingRequest extends SlingHttpServletRequestWrapper {
		private final Resource currentResource;
		private final SlingBindings bindings;
		private final String path;
		private final AdapterManager adapterManager;

		private ReactSlingRequest(AdapterManager adapterManager, SlingHttpServletRequest wrappedRequest,
				Resource currentResource, SlingBindings bindings, String path) {
			super(wrappedRequest);
			this.currentResource = currentResource;
			this.bindings = bindings;
			this.path = path;
			this.adapterManager = adapterManager;
		}

		@Override
		public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
			return adapterManager.getAdapter(this, type);
		}

		@Override
		public Object getAttribute(String name) {
			if (SlingBindings.class.getName().equals(name)) {
				return bindings;
			}
			return super.getAttribute(name);
		}

		@Override
		public RequestPathInfo getRequestPathInfo() {
			return new ResourcePathInfoWrapper(super.getRequestPathInfo(), path);
		}

		@Override
		public Resource getResource() {

			return currentResource;
		}
	}

	@SuppressWarnings("all")
	private static class ResourcePathInfoWrapper implements RequestPathInfo {

		private RequestPathInfo wrapped;

		public ResourcePathInfoWrapper(RequestPathInfo wrapped, String path) {
			super();
			this.wrapped = wrapped;
			this.path = path;
		}

		private String path;

		@Override
		public String getExtension() {
			return wrapped.getExtension();
		}

		@Override
		public String getSelectorString() {
			return wrapped.getSelectorString();
		}

		@Override
		public String[] getSelectors() {
			return wrapped.getSelectors();
		}

		@Override
		public String getSuffix() {
			return wrapped.getSuffix();
		}

		@Override
		public Resource getSuffixResource() {
			return wrapped.getSuffixResource();
		}

		@Override
		public String getResourcePath() {
			return path;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelFactory.class);

	private ClassLoader classLoader;

	private org.apache.sling.models.factory.ModelFactory modelFactory;

	private AdapterManager adapterManager;

	private ObjectMapper mapper;

	public ModelFactory(ClassLoader classLoader, SlingHttpServletRequest request,
			org.apache.sling.models.factory.ModelFactory modelFactory, AdapterManager adapterManager,
			ObjectMapper mapper, ResourceResolver resourceResolver) {
		super();
		this.classLoader = classLoader;
		this.request = request;
		this.modelFactory = modelFactory;
		this.adapterManager = adapterManager;
		this.mapper = mapper;
		this.resourceResolver = resourceResolver;
	}

	private ResourceResolver resourceResolver;
	private SlingHttpServletRequest request;

	private Resource getResource(String path) {

		final Resource currentResource = resourceResolver.resolve(request, path);

		if (currentResource == null) {
			return new NonExistingResource(resourceResolver, path);
		}
		return currentResource;

	}

	/**
	 * adapts the current request to the given class
	 *
	 * @param className
	 *            fully qualified class name
	 * @return
	 */
	public JsProxy createRequestModel(String path, String className) {

		final Resource currentResource = getResource(path);
		SlingBindings originalBindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
		SlingBindings bindings = new SlingBindings();
		SlingHttpServletRequestWrapper newRequest = new ReactSlingRequest(adapterManager, request, currentResource,
				bindings, currentResource.getPath());
		if (originalBindings != null) {
			bindings.setFlush(originalBindings.getFlush());
			bindings.setLog(originalBindings.getLog());
			bindings.setOut(originalBindings.getOut());
			bindings.setReader(originalBindings.getReader());
			bindings.setRequest(newRequest);
			bindings.setResource(currentResource);
			bindings.setResponse(originalBindings.getResponse());
			bindings.setSling(originalBindings.getSling());

		}

		return createModel(className, newRequest);
	}

	/**
	 * adapts the current resource to the given class
	 *
	 * @param className
	 *            fully qualified class name
	 * @return
	 */
	public JsProxy createResourceModel(String path, String className) {
		Resource resource = resourceResolver.resolve(path);
		if (resource == null) {
			return null;
		}
		return createModel(className, resource);
	}

	private JsProxy createModel(String className, Adaptable adaptable) {

		Class<?> clazz;
		try {
			clazz = classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			LOGGER.error("could not find model class " + className);
			return null;
		}
		return MetricsHelper.getCurrent().timer("model", () -> {
			Object object = modelFactory.createModel(adaptable, clazz);

			if (object == null) {
				return null;
			}
			return new JsProxy(object, clazz, mapper);
		});
	}

}
