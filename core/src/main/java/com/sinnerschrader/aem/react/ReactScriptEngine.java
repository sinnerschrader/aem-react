package com.sinnerschrader.aem.react;

import java.io.Reader;
import java.util.Arrays;
import java.util.NoSuchElementException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.sling.JsonObjectCreator;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.api.ModelFactory;
import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.exception.TechnicalException;

public class ReactScriptEngine extends AbstractSlingScriptEngine {

  public interface Command {
    public Object execute(JavascriptEngine e);
  }

  private static final String SERVER_RENDERING_DISABLED = "disabled";
  private static final String SERVER_RENDERING_PARAM = "serverRendering";
  private static final Logger LOG = LoggerFactory.getLogger(ReactScriptEngine.class);
  private ObjectPool<JavascriptEngine> enginePool;
  private boolean reloadScripts;
  private ObjectMapper mapper;
  private OsgiServiceFinder finder;
  private DynamicClassLoaderManager dynamicClassLoaderManager;

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
  }

  protected ReactScriptEngine(ScriptEngineFactory scriptEngineFactory, ObjectPool<JavascriptEngine> enginePool, boolean reloadScripts, OsgiServiceFinder finder,
      DynamicClassLoaderManager dynamicClassLoaderManager) {
    super(scriptEngineFactory);

    this.mapper = new ObjectMapper();
    mapper.configure(Feature.IGNORE_UNKNOWN, true);

    this.enginePool = enginePool;
    this.reloadScripts = reloadScripts;
    this.finder = finder;
    this.dynamicClassLoaderManager = dynamicClassLoaderManager;
  }

  @Override
  public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
    ClassLoader old = Thread.currentThread().getContextClassLoader();
    try {

      Thread.currentThread().setContextClassLoader(((ReactScriptEngineFactory) getFactory()).getClassLoader());

      Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
      SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
      SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
      boolean renderAsJson = Arrays.asList(request.getRequestPathInfo().getSelectors()).indexOf("json") >= 0;
      Resource resource = request.getResource();

      boolean dialog = request.getAttribute(Sling.ATTRIBUTE_AEM_REACT_DIALOG) != null;

      if (dialog) {
        // just rendering to get the wrapper element and author mode js
        scriptContext.getWriter().write("");
        return null;
      }

      String renderedHtml;
      boolean serverRendering = !SERVER_RENDERING_DISABLED.equals(request.getParameter(SERVER_RENDERING_PARAM));
      String cacheString = null;
      if (serverRendering) {
        RenderResult result = renderReactMarkup(resource.getPath(), resource.getResourceType(), getWcmMode(request), scriptContext, renderAsJson);
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
        resources.put(resource.getPath(), resourceEntry);
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
        output = wrapHtml(resource.getPath(), resource, renderedHtml, serverRendering, getWcmMode(request), cacheString);

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
  private String wrapHtml(String path, Resource resource, String renderedHtml, boolean serverRendering, String wcmmode, String cache) {
    JSONObject reactProps = new JSONObject();
    try {
      if (cache != null) {
        reactProps.put("cache", new JSONObject(cache));
      }
      reactProps.put("resourceType", resource.getResourceType());
      reactProps.put("path", resource.getPath());
      reactProps.put("wcmmode", wcmmode);
    } catch (JSONException e) {
      throw new TechnicalException("cannot create react props", e);
    }
    String allHtml = "<div data-react-server=\"" + String.valueOf(serverRendering) + "\" data-react=\"app\" data-react-id=\"" + path + "_component\">"
            + renderedHtml + "</div>"
            + "<script id=\"" + path + "_component\" type=\"application/json\">" + reactProps.toString() + "</script>";

    return allHtml;
  }

  private Cqx createCqx(ScriptContext ctx) {
    SlingHttpServletRequest request = (SlingHttpServletRequest) ctx.getBindings(ScriptContext.ENGINE_SCOPE).get(SlingBindings.REQUEST);

    ClassLoader classLoader = dynamicClassLoaderManager.getDynamicClassLoader();
    return new Cqx(new Sling(ctx), finder, new ModelFactory(classLoader, request));
  }

  /**
   * render the react markup
   *
   * @param reactProps
   *          props
   * @param component
   *          component name
   * @return
   */
  private RenderResult renderReactMarkup(String path, String resourceType, String wcmmode, ScriptContext scriptContext, boolean renderRootDialog) {
    JavascriptEngine javascriptEngine;
    try {
      javascriptEngine = enginePool.borrowObject();
      try {
        if (reloadScripts) {
          javascriptEngine.reloadScripts();
        }
        return javascriptEngine.render(path, resourceType, wcmmode, createCqx(scriptContext), renderRootDialog);
      } finally {

        enginePool.returnObject(javascriptEngine);

      }
    } catch (NoSuchElementException e) {
      throw new TechnicalException("cannot get engine from pool", e);
    } catch (IllegalStateException e) {
      throw new TechnicalException("cannot return engine from pool", e);
    } catch (Exception e) {
      throw new TechnicalException("error rendering react markup", e);
    }

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
