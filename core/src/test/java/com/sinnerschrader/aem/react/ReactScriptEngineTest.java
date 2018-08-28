package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;

import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.cache.ComponentCache;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;

@RunWith(MockitoJUnitRunner.class)
public class ReactScriptEngineTest {

	@Rule
	public SlingContext slingContext = new SlingContext();

	@Mock
	private ScriptCollectionLoader loader;

	@Mock
	private ReactScriptEngineFactory factory;

	@Mock
	private ResourceResolver resourceResolver;

	@Mock
	private ClassLoader classLoader;

	@Mock
	private DynamicClassLoaderManager dynamicClassLoaderManager;

	@Mock
	private SlingScriptHelper sling;

	@Mock
	private JavascriptEngine engine;

	private ScriptContext scriptContext;

	private ObjectNode getJsonFromTextArea(Element ta) throws IOException, JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode json = (ObjectNode) objectMapper.readTree(ta.html());
		return json;
	}

	private Element getWrapper(Document doc) {
		Elements es = doc.select("[data-react=\"app\"]");
		return es.get(0);
	}

	private Element getTextarea(Document doc) {
		Elements es = doc.select("textarea");
		return es.get(0);
	}

	@Before
	public void setup() {
		Mockito.when(factory.getClassLoader()).thenReturn(classLoader);
		scriptContext = new SimpleScriptContext();
		StringWriter writer = new StringWriter();
		scriptContext.setWriter(writer);
		Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put(SlingBindings.REQUEST, slingContext.request());
		bindings.put(SlingBindings.RESPONSE, slingContext.response());
		bindings.put(SlingBindings.SLING, slingContext.slingScriptHelper());
	}

	@Test
	@Ignore("fix me")
	public void testEval() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, loader, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, false, false,
				new ComponentCache(null, null, 0, 0, null, false));

		RenderResult result = expectResult();

		String resourceType = "/apps/test";
		String path = "/content/page/test";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType);
		slingContext.currentResource(resource);

		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = getRenderedHtml();

		Document doc = Jsoup.parse(renderedHtml);

		Element wrapper = getWrapper(doc);

		Assert.assertEquals("test xxx", wrapper.attr("class"));
		Assert.assertEquals("span", wrapper.nodeName());

		Element textarea = getTextarea(doc);
		ObjectNode jsonFromTextArea = getJsonFromTextArea(textarea);
		Assert.assertTrue(wrapper.html().startsWith(result.html));
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals(result.cache, jsonFromTextArea.get("cache").toString());

	}

	private RenderResult expectResult() throws Exception {
		RenderResult result = new RenderResult();
		result.cache = "{\"cache\":true}";
		result.html = "<div></div>";
		return result;
	}

	@Test
	@Ignore("fix me")
	public void testEvalDisableMapping() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, loader, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

		RenderResult result = expectResult();

		String resourceType = "/apps/test";
		String path = "/content/page/test";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType);
		slingContext.currentResource(resource);

		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = getRenderedHtml();

		Document doc = Jsoup.parse(renderedHtml);

		Element wrapper = getWrapper(doc);

		Assert.assertEquals("test xxx", wrapper.attr("class"));
		Assert.assertEquals("span", wrapper.nodeName());

		Element textarea = getTextarea(doc);
		ObjectNode jsonFromTextArea = getJsonFromTextArea(textarea);
		Assert.assertTrue(wrapper.html().startsWith(result.html));
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals(result.cache, jsonFromTextArea.get("cache").toString());

	}

	@Test
	public void testEvalServerRenderingDisabled() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, loader, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

		RenderResult result = expectResult();

		String resourceType = "/apps/test";
		String path = "/content/page/test";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType);
		slingContext.currentResource(resource);

		slingContext.request().setQueryString("serverRendering=disabled");

		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = getRenderedHtml();

		Document doc = Jsoup.parse(renderedHtml);

		Element wrapper = getWrapper(doc);

		Assert.assertEquals("test xxx", wrapper.attr("class"));
		Assert.assertEquals("span", wrapper.nodeName());

		Element textarea = getTextarea(doc);
		ObjectNode jsonFromTextArea = getJsonFromTextArea(textarea);
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());

	}

	@Test
	public void testEvalWrapperElement() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, loader, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

		String resourceType = "/apps/test";
		String path = "/content/page/test";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType);
		slingContext.currentResource(resource);
		slingContext.request().setAttribute(Sling.ATTRIBUTE_AEM_REACT_DIALOG, true);

		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = getRenderedHtml();

		Assert.assertEquals("", renderedHtml);

	}

	@Test
	@Ignore
	public void testEvalJsonOnly() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, loader,null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

		slingContext.requestPathInfo().setSelectorString("json");

		RenderResult result = expectResult();

		String resourceType = "/apps/test";
		String path = "/content/page/test";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType);
		slingContext.currentResource(resource);

		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = getRenderedHtml();

		Assert.assertEquals("{\"cache\":true}", renderedHtml);

	}

	@Test
	public void testEvalJsonOnlyNoServerRendering() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, loader, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

		slingContext.requestPathInfo().setSelectorString("json");
		slingContext.request().setQueryString("serverRendering=disabled");

		RenderResult result = expectResult();

		String resourceType = "/apps/test";
		String path = "/content/page/test";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType);
		slingContext.currentResource(resource);

		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = getRenderedHtml();

		Assert.assertEquals(
				"{\"resources\":{\"/content/page/test\":{\"depth\":-1,\"data\":{\"sling:resourceType\":\"/apps/test\"}}}}",
				renderedHtml);

	}

	@Test
	@Ignore("needs to be fixed")
	public void testEvalNoIncomingMapping() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, loader, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), true, false, false,
				new ComponentCache(null, null, 0, 0, null, false));

		RenderResult result = expectResult();

		String resourceType = "/apps/test";
		String path = "/content/page/test";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType);
		slingContext.currentResource(resource);

		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = getRenderedHtml();

		Document doc = Jsoup.parse(renderedHtml);

		Element wrapper = getWrapper(doc);

		Assert.assertEquals("test xxx", wrapper.attr("class"));
		Assert.assertEquals("span", wrapper.nodeName());

		Element textarea = getTextarea(doc);
		ObjectNode jsonFromTextArea = getJsonFromTextArea(textarea);
		Assert.assertTrue(wrapper.html().startsWith(result.html));
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals(result.cache, jsonFromTextArea.get("cache").toString());

	}

	private String getRenderedHtml() {
		return ((StringWriter) scriptContext.getWriter()).getBuffer().toString();
	}
}
