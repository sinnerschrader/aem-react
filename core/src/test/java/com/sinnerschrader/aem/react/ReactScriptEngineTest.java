package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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
	private ReactScriptEngineFactory factory;

	@Mock
	private ClassLoader classLoader;

	@Mock
	private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Mock
    private PoolManager poolManager;

	private ScriptContext scriptContext;

	private ObjectNode getJsonFromTextArea(Element ta) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return (ObjectNode) objectMapper.readTree(ta.html());
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
	public void setup() throws Exception {
		Mockito.when(factory.getClassLoader()).thenReturn(classLoader);
		scriptContext = new SimpleScriptContext();
		StringWriter writer = new StringWriter();
		scriptContext.setWriter(writer);
		Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put(SlingBindings.REQUEST, slingContext.request());
		bindings.put(SlingBindings.RESPONSE, slingContext.response());
		bindings.put(SlingBindings.SLING, slingContext.slingScriptHelper());
		Mockito.when(poolManager.execute(Mockito.any( PoolManager.EngineUser.class ))).thenReturn(expectResult());
	}

	@Test
	public void testEval() throws Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager, null, dynamicClassLoaderManager,
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
		Assert.assertTrue(wrapper.html().replaceAll("\\p{C}| ", "").startsWith(result.html));
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals(result.cache, jsonFromTextArea.get("cache").toString());

	}

	private RenderResult expectResult() {
		RenderResult result = new RenderResult();
		result.cache = "{\"cache\":true}";
		result.html = "<div>result</div>";
		return result;
	}

	@Test
	public void testEvalDisableMapping() throws Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager, null, dynamicClassLoaderManager,
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
		Assert.assertTrue(wrapper.html().replaceAll("\\p{C}| ", "").startsWith(result.html));
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals(result.cache, jsonFromTextArea.get("cache").toString());

	}

	@Test
	public void testEvalServerRenderingDisabled() throws Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

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
	public void testEvalWrapperElement() throws Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager, null, dynamicClassLoaderManager,
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
	public void testEvalJsonOnly() throws Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager,null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

		slingContext.requestPathInfo().setSelectorString("json");

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
	public void testEvalJsonOnlyNoServerRendering() throws Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager, null, dynamicClassLoaderManager,
				"span", "test xxx", null, null, null, new ComponentMetricsService(), false, true, false,
				new ComponentCache(null, null, 0, 0, null, false));

		slingContext.requestPathInfo().setSelectorString("json");
		slingContext.request().setQueryString("serverRendering=disabled");

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
	public void testEvalNoIncomingMapping() throws Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager, null, dynamicClassLoaderManager,
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
		Assert.assertTrue(wrapper.html().replaceAll("\\p{C}| ", "").startsWith(result.html));
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals(result.cache, jsonFromTextArea.get("cache").toString());

	}

	private String getRenderedHtml() {
		return ((StringWriter) scriptContext.getWriter()).getBuffer().toString();
	}
}
