package com.sinnerschrader.aem.react;

import java.io.Reader;
import java.util.Arrays;
import java.util.NoSuchElementException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.sling.JsonObjectCreator;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.xss.XSSAPI;
import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.api.ModelFactory;
import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.json.ResourceMapper;
import com.sinnerschrader.aem.react.json.ResourceMapperLocator;

public class ReactScriptEngine extends AbstractSlingScriptEngine {

	private static final String REACT_CONTEXT_KEY = "com.sinnerschrader.aem.react.ReactContext";

	public interface Command {
		public Object execute(JavascriptEngine e);
	}

	private static final String SERVER_RENDERING_DISABLED = "disabled";
	private static final String SERVER_RENDERING_PARAM = "serverRendering";
	private static final Logger LOG = LoggerFactory.getLogger(ReactScriptEngine.class);
	private ObjectPool<JavascriptEngine> enginePool;
	private OsgiServiceFinder finder;
	private DynamicClassLoaderManager dynamicClassLoaderManager;
	private String rootElementName;
	private String rootElementClass;
	private org.apache.sling.models.factory.ModelFactory modelFactory;
	private AdapterManager adapterManager;
	private ObjectMapper mapper;

	/**
	 * This class is the result of rendering a react component(-tree). It consists
	 * of html and cache.
	 *
	 * @author stemey
	 *
	 */
	public static class RenderResult {
		public String html;
		public String cache;
		public Object reactContext;
	}

	protected ReactScriptEngine(ReactScriptEngineFactory scriptEngineFactory, ObjectPool<JavascriptEngine> enginePool,
			OsgiServiceFinder finder, DynamicClassLoaderManager dynamicClassLoaderManager, String rootElementName,
			String rootElementClass, org.apache.sling.models.factory.ModelFactory modelFactory,
			AdapterManager adapterManager, ObjectMapper mapper) {
		super(scriptEngineFactory);
		this.adapterManager = adapterManager;
		this.enginePool = enginePool;
		this.finder = finder;
		this.dynamicClassLoaderManager = dynamicClassLoaderManager;
		this.rootElementName = rootElementName;
		this.rootElementClass = rootElementClass;
		this.modelFactory = modelFactory;
		this.mapper = mapper;
	}

	@Override
	public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
		ClassLoader old = Thread.currentThread().getContextClassLoader();
		try {

			Thread.currentThread().setContextClassLoader(((ReactScriptEngineFactory) getFactory()).getClassLoader());

			Bindings bindings = getBindings(scriptContext);
			SlingScriptHelper sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
			SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
			SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
			boolean renderAsJson = Arrays.asList(request.getRequestPathInfo().getSelectors()).indexOf("json") >= 0;
			Resource resource = request.getResource();

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
			boolean serverRendering = !SERVER_RENDERING_DISABLED.equals(request.getParameter(SERVER_RENDERING_PARAM));
			String cacheString = null;
			String path = resource.getPath();
			String mappedPath = request.getResourceResolver().map(request, path);
			if (serverRendering) {
				final Object reactContext = request.getAttribute(REACT_CONTEXT_KEY);
				RenderResult result = renderReactMarkup(mappedPath, resource.getResourceType(), getWcmMode(request),
						scriptContext, renderAsJson, reactContext);
				renderedHtml = result.html;
				cacheString = result.cache;
				request.setAttribute(REACT_CONTEXT_KEY, result.reactContext);
			} else if (renderAsJson) {
				// development mode: return cache with just the current
				// resource.
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
						cacheString);

			}

			scriptContext.getWriter().write(output);
			return null;

		} catch (Exception e) {
			throw new ScriptException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(old);
		}

	}

	/**
	 * wrap the rendered react markup with the teaxtarea that contains the
	 * component's props.
	 *
	 * @param path
	 * @param reactProps
	 * @param component
	 * @param renderedHtml
	 * @param serverRendering
	 * @return
	 */
	String wrapHtml(String mappedPath, Resource resource, String renderedHtml, boolean serverRendering, String wcmmode,
			String cache) {
		JSONObject reactProps = new JSONObject();
		try {
			if (cache != null) {
				reactProps.put("cache", new JSONObject(cache));
			}
			reactProps.put("resourceType", resource.getResourceType());
			reactProps.put("path", mappedPath);
			reactProps.put("wcmmode", wcmmode);
		} catch (JSONException e) {
			throw new TechnicalException("cannot create react props", e);
		}
		String jsonProps = StringEscapeUtils.escapeHtml4(reactProps.toString());
		String classString = (StringUtils.isNotEmpty(rootElementClass)) ? " class=\"" + rootElementClass + "\"" : "";
		String allHtml = "<" + rootElementName + " " + classString + " data-react-server=\""
				+ String.valueOf(serverRendering) + "\" data-react=\"app\" data-react-id=\"" + mappedPath
				+ "_component\">" + renderedHtml + "</" + rootElementName + ">" + "<textarea id=\"" + mappedPath
				+ "_component\" style=\"display:none;\">" + jsonProps + "</textarea>";

		return allHtml;
	}

	private Cqx createCqx(ScriptContext ctx) {
		SlingHttpServletRequest request = (SlingHttpServletRequest) getBindings(ctx).get(SlingBindings.REQUEST);
		SlingScriptHelper sling = (SlingScriptHelper) getBindings(ctx).get(SlingBindings.SLING);

		ClassLoader classLoader = dynamicClassLoaderManager.getDynamicClassLoader();
		ModelFactory reactModelFactory = new ModelFactory(classLoader, request, modelFactory, adapterManager, mapper);
		return new Cqx(new Sling(ctx), finder, reactModelFactory, sling.getService(XSSAPI.class), mapper);
	}

	/**
	 * render the react markup
	 *
	 * @param reactProps
	 *            props
	 * @param component
	 *            component name
	 * @return
	 */
	private RenderResult renderReactMarkup(String mappedPath, String resourceType, String wcmmode,
			ScriptContext scriptContext, boolean renderAsJson, Object reactContext) {
		JavascriptEngine javascriptEngine;
		boolean removeMapper = false;
		try {
			SlingHttpServletRequest request = getRequest(getBindings(scriptContext));
			ResourceMapper resourceMapper = new ResourceMapper(request);
			removeMapper = ResourceMapperLocator.setInstance(resourceMapper);
			javascriptEngine = enginePool.borrowObject();
			try {
				while (javascriptEngine.isScriptsChanged()) {
					LOG.info("scripts changed -> invalidate engine");
					enginePool.invalidateObject(javascriptEngine);
					javascriptEngine = enginePool.borrowObject();
				}
				return javascriptEngine.render(mappedPath, resourceType, wcmmode, createCqx(scriptContext),
						renderAsJson, reactContext);
			} finally {

				try {
					if (javascriptEngine != null) {
						enginePool.returnObject(javascriptEngine);
					}
				} catch (IllegalStateException e) {
					// returned object that is not in the pool any more
				}

			}
		} catch (NoSuchElementException e) {
			LOG.info("engine pool exhausted");
			throw new TechnicalException("cannot get engine from pool", e);
		} catch (IllegalStateException e) {
			throw new TechnicalException("cannot return engine from pool", e);
		} catch (Exception e) {
			throw new TechnicalException("error rendering react markup", e);
		} finally {
			if (removeMapper) {
				ResourceMapperLocator.clearInstance();
			}
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
		enginePool.close();
	}

	public static void main(String[] args) {
		System.out.println(Text.escapeIllegalJcrChars("[]"));
	}

}
