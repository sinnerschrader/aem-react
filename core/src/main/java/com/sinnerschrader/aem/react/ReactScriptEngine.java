package com.sinnerschrader.aem.react;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.sling.JsonObjectCreator;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;

import com.adobe.granite.xss.XSSAPI;
import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.api.ModelFactory;
import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.cache.CacheKey;
import com.sinnerschrader.aem.react.cache.ComponentCache;
import com.sinnerschrader.aem.react.cache.ModelCollector;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.json.ReactSlingHttpServletRequestWrapper;
import com.sinnerschrader.aem.react.json.ResourceMapper;
import com.sinnerschrader.aem.react.json.ResourceMapperLocator;
import com.sinnerschrader.aem.react.mapping.ResourceResolverHelperFactory;
import com.sinnerschrader.aem.react.mapping.ResourceResolverUtils;
import com.sinnerschrader.aem.react.metrics.ComponentMetrics;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactScriptEngine extends AbstractSlingScriptEngine {

	public interface Command {
		Object execute(JavascriptEngine e);
	}

	/**
	 * This class is the result of rendering a react component(-tree). It consists
	 * of html and cache.
	 *
	 * @author stemey
	 */
	public static class RenderResult {
		public String html;
		public String cache;
	}

	private static final Logger log = LoggerFactory.getLogger(ReactScriptEngine.class);

	private static final String JSON_RENDER_SELECTOR = "json";
	private static final String REACT_CONTEXT_KEY = "com.sinnerschrader.aem.react.ReactContext";
	public static final String REACT_ROOT_NO_KEY = "com.sinnerschrader.aem.react.RootNo";

	private static final String SERVER_RENDERING_DISABLED = "disabled";
	private static final String SERVER_RENDERING_PARAM = "serverRendering";

	private final OsgiServiceFinder finder;
	private final DynamicClassLoaderManager dynamicClassLoaderManager;
	private final String rootElementName;
	private final String rootElementClass;
	private final org.apache.sling.models.factory.ModelFactory modelFactory;
	private final AdapterManager adapterManager;
	private final ObjectMapper mapper;
	private final ComponentMetricsService metricsService;
	private final boolean disableMapping;
	private final boolean enableReverseMapping;
	private final boolean mangleNameSpaces;
	private final ComponentCache cache;
	private final PoolManager poolManager;

	protected ReactScriptEngine(ReactScriptEngineFactory scriptEngineFactory, PoolManager poolManager,
			OsgiServiceFinder finder, DynamicClassLoaderManager dynamicClassLoaderManager, String rootElementName,
			String rootElementClass, org.apache.sling.models.factory.ModelFactory modelFactory,
			AdapterManager adapterManager, ObjectMapper mapper, ComponentMetricsService metricsService,
			boolean enableReverseMapping, boolean disableMapping, boolean mangleNameSpaces, ComponentCache cache) {
		super(scriptEngineFactory);
		this.adapterManager = adapterManager;
		this.poolManager = poolManager;
		this.finder = finder;
		this.dynamicClassLoaderManager = dynamicClassLoaderManager;
		this.rootElementName = rootElementName;
		this.rootElementClass = rootElementClass;
		this.modelFactory = modelFactory;
		this.mapper = mapper;
		this.metricsService = metricsService;
		this.disableMapping = disableMapping;
		this.enableReverseMapping = enableReverseMapping;
		this.mangleNameSpaces = mangleNameSpaces;
		this.cache = cache;
	}

	@Override
	public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
		long start = System.currentTimeMillis();
		ClassLoader old = Thread.currentThread().getContextClassLoader();

		Bindings bindings = getBindings(scriptContext);
		SlingScriptHelper sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
		SlingHttpServletRequest originalRequest = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
		SlingHttpServletRequest request;

		String resourceType = null;

		if (enableReverseMapping) {
			ResourceResolver resolverHelper = ResourceResolverHelperFactory.create(originalRequest, mangleNameSpaces);
			request = new ReactSlingHttpServletRequestWrapper(originalRequest, resolverHelper);
			bindings.put(SlingBindings.REQUEST, request);
		} else {
			request = originalRequest;
		}
		SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
		try {
			Thread.currentThread().setContextClassLoader(((ReactScriptEngineFactory) getFactory()).getClassLoader());

			List<String> rawSelectors = Arrays.asList(request.getRequestPathInfo().getSelectors());

			final List<String> selectors;
			boolean renderAsJson = rawSelectors.indexOf(JSON_RENDER_SELECTOR) >= 0;
			if (renderAsJson) {
				selectors = rawSelectors.stream().filter((String selector) -> !JSON_RENDER_SELECTOR.equals(selector))
						.collect(Collectors.toList());
			} else {
				selectors = rawSelectors;
			}
			Resource resource = request.getResource();
			resourceType = resource.getResourceType();

			try (ComponentMetrics metrics = metricsService.create(resource)) {
				SlingBindings slingBindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
				if (slingBindings == null) {
					slingBindings = new SlingBindings();
					slingBindings.setSling(sling);
					request.setAttribute(SlingBindings.class.getName(), slingBindings);
				}

				boolean dialog = request.getAttribute(Sling.ATTRIBUTE_AEM_REACT_DIALOG) != null;

				if (dialog) {
					// just rendering to get the wrapper element and author mode js
					scriptContext.getWriter().write("");
					return null;
				}

				String renderedHtml;
				boolean serverRendering = !SERVER_RENDERING_DISABLED
						.equals(request.getParameter(SERVER_RENDERING_PARAM));
				String cacheString = null;
				String path = resource.getPath();
				String mappedPath;

				if (!disableMapping) {
					mappedPath = request.getResourceResolver().map(request, path);
					mappedPath = ResourceResolverUtils.getUriPath(mappedPath);
				} else {
					mappedPath = path;
				}

				if (serverRendering) {
					final String reactContext = (String) request.getAttribute(REACT_CONTEXT_KEY);
					Integer rootNo = (Integer) request.getAttribute(REACT_ROOT_NO_KEY);
					if (rootNo == null) {
						rootNo = 0;
					} else {
						rootNo = rootNo + 1;
					}
					request.setAttribute(REACT_ROOT_NO_KEY, rootNo);
					RenderResult result = renderReactMarkup(mappedPath, resource.getResourceType(), rootNo,
							getWcmMode(request), scriptContext, renderAsJson, reactContext, selectors);
					renderedHtml = result.html;
					cacheString = result.cache;
				} else if (renderAsJson) {
					// development mode: return cache with just the current resource.
					JSONObject cache = new JSONObject();
					JSONObject resources = new JSONObject();
					JSONObject resourceEntry = new JSONObject();
					resourceEntry.put("depth", -1);
					// depth is inaccurate
					resourceEntry.put("data", JsonObjectCreator.create(resource, -1));
					resources.put(mappedPath, resourceEntry);
					cache.put("resources", resources);
					cacheString = cache.toString();
					renderedHtml = "";
				} else {
					// initial rendering in development mode
					renderedHtml = "";
				}

				String output;
				if (renderAsJson) {
					output = cacheString;
					response.setContentType("application/json");
				} else {
					output = wrapHtml(mappedPath, resource, renderedHtml, serverRendering, getWcmMode(request),
							cacheString, selectors);
				}

				scriptContext.getWriter().write(output);
				return null;
			}

		} catch (Exception e) {
			throw new ScriptException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(old);

			long elapsed = System.currentTimeMillis() - start;

			MdcUtil.addToMdc(start, "eval", elapsed >= 1000 ? resourceType : null);

			log.debug("React rendering of resource type {} took {} ms", resourceType, elapsed);
		}
	}

	/**
	 * wrap the rendered react markup with the teaxtarea that contains the
	 * component's props.
	 *
	 * @param mappedPath
	 * @param resource
	 * @param renderedHtml
	 * @param serverRendering
	 * @param wcmmode
	 * @param cache
	 * @param selectors
	 * @return
	 */
	String wrapHtml(String mappedPath, Resource resource, String renderedHtml, boolean serverRendering, String wcmmode,
			String cache, List<String> selectors) {
		JSONObject reactProps = new JSONObject();
		try {
			if (cache != null) {
				reactProps.put("cache", new JSONObject(cache));
			}
			reactProps.put("resourceType", resource.getResourceType());
			reactProps.put("selectors", selectors);
			reactProps.put("path", mappedPath);
			reactProps.put("wcmmode", wcmmode);
		} catch (JSONException e) {
			throw new TechnicalException("cannot create react props", e);
		}
		String classString = StringUtils.isNotEmpty(rootElementClass)
				? " class=\"" + rootElementClass + "\""
				: "";
		String allHtml = "<" + rootElementName + " " + classString + " data-react-server=\"" + serverRendering + "\" data-react=\"app\" >" + renderedHtml + "</" + rootElementName + ">"
				+ "<script type=\"application/json\">" + reactProps.toString() + "</script>";

		return allHtml;
	}

	protected Cqx createCqx(ModelCollector modelCollector, ScriptContext ctx) {
		SlingHttpServletRequest request = (SlingHttpServletRequest) getBindings(ctx).get(SlingBindings.REQUEST);
		SlingScriptHelper sling = (SlingScriptHelper) getBindings(ctx).get(SlingBindings.SLING);

		ClassLoader classLoader = dynamicClassLoaderManager.getDynamicClassLoader();
		ModelFactory reactModelFactory = new ModelFactory(modelCollector, classLoader, request, modelFactory,
				adapterManager, mapper, request.getResourceResolver());
		return new Cqx(new Sling(ctx), finder, reactModelFactory, sling.getService(XSSAPI.class), mapper);
	}

	/**
	 * render the react markup
	 *
	 * @param mappedPath
	 * @param resourceType
	 * @param wcmmode
	 * @param scriptContext
	 * @param renderAsJson
	 * @param reactContext
	 * @param selectors
	 * @return
	 * @throws Exception
	 */
	private RenderResult renderReactMarkup(String mappedPath, String resourceType, int rootNo, String wcmmode,
			ScriptContext scriptContext, boolean renderAsJson, String reactContext, List<String> selectors)
			throws Exception {
		SlingHttpServletRequest request = getRequest(getBindings(scriptContext));
		final ModelCollector collector = new ModelCollector();
		ResourceMapper resourceMapper = new ResourceMapper(request);
		ResourceMapper replacedResourceMapper = null;
		try {
			replacedResourceMapper = ResourceMapperLocator.setInstance(resourceMapper);
			return cache.cache(collector, new CacheKey(mappedPath, resourceType, wcmmode, renderAsJson, selectors),
					request, mappedPath, resourceType, () -> poolManager.execute((ReactRenderEngine engine) -> {

						try {
							Cqx cqx = createCqx(collector, scriptContext);
							return engine.render(mappedPath, resourceType, rootNo, wcmmode, cqx, renderAsJson,
									selectors);
						} catch (Exception e) {
							throw new TechnicalException("error rendering react markup", e);
						}
					}));
		} finally {
			ResourceMapperLocator.setInstance(replacedResourceMapper);
		}
	}

	private SlingHttpServletRequest getRequest(Bindings bindings) {
		return (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
	}

	private Bindings getBindings(ScriptContext scriptContext) {
		return scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
	}

	private String getWcmMode(SlingHttpServletRequest request) {
		return WCMMode.fromRequest(request).name().toLowerCase();
	}

	public void stop() {
		poolManager.close();
	}

}
