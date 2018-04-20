package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import javax.servlet.RequestDispatcher;

import org.apache.commons.io.IOUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.servlethelpers.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.integration.TextProps;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationTest {

	@Mock
	private ReactScriptEngineFactory factory;
	@Mock
	private DynamicClassLoaderManager dynamicClassLoaderManager;

	@Mock
	private ScriptCollectionLoader loader;

	@Mock
	private MockRequestDispatcherFactory requestDispatcherFactory;

	@Mock
	private ObjectPool<JavascriptEngine> enginePool;

	@Mock
	private SlingScriptHelper sling;

	@Mock
	private ModelFactory modelFactory;

	private ObjectMapper mapper = new ObjectMapper();

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

	@Rule
	public SlingContext slingContext = new SlingContext();

	@Mock
	private RequestDispatcher dispatcher;

	@Test
	public void testRenderText() throws NoSuchElementException, IllegalStateException, Exception {
		String resourceType = "react-demo/components/text";
		String path = "/content/page/test";
		String content = "Hallo";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType, "content",
				content);
		slingContext.currentResource(resource);

		String html = render();
		Document doc = Jsoup.parse(html);

		Element wrapper = getWrapper(doc);

		Assert.assertEquals("test xxx", wrapper.attr("class"));
		Assert.assertEquals("span", wrapper.nodeName());

		Element textarea = getTextarea(doc);
		ObjectNode jsonFromTextArea = getJsonFromTextArea(textarea);
		Assert.assertEquals("<span data-reactroot=\"\">Hallo</span>", wrapper.html());
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals(content, jsonFromTextArea.get("cache").get("transforms").get(path).get("content").asText());
		Assert.assertEquals(path + "_component", wrapper.attr("data-react-id"));
		Assert.assertEquals(path + "_component", textarea.attr("id"));

	}

	@Test
	public void testRenderTextRequestModel() throws NoSuchElementException, IllegalStateException, Exception {
		String resourceType = "react-demo/components/text";
		String path = "/content/page/test";
		String content = "Hallo";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType, "content",
				content);
		slingContext.currentResource(resource);
		slingContext.requestPathInfo().setSelectorString("requestmodel");
		Mockito.when(modelFactory.createModel(Mockito.any(SlingHttpServletRequest.class), Mockito.eq(TextProps.class)))
				.thenReturn(new TextProps());

		String html = render();
		Document doc = Jsoup.parse(html);

		Element wrapper = getWrapper(doc);

		Assert.assertEquals("test xxx", wrapper.attr("class"));
		Assert.assertEquals("span", wrapper.nodeName());

		Element textarea = getTextarea(doc);
		ObjectNode jsonFromTextArea = getJsonFromTextArea(textarea);
		Assert.assertEquals("<span data-reactroot=\"\">RequestModel</span>", wrapper.html());
		Assert.assertEquals(resourceType, jsonFromTextArea.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromTextArea.get("path").asText());
		Assert.assertEquals("RequestModel",
				jsonFromTextArea.get("cache").get("transforms").get(path + ":requestmodel").get("content").asText());
		Assert.assertEquals(path + "_component", wrapper.attr("data-react-id"));
		Assert.assertEquals(path + "_component", textarea.attr("id"));

	}

	@Test
	public void testDialog() throws NoSuchElementException, IllegalStateException, Exception {
		String resourceType = "react-demo/components/text";
		String path = "/content/page/test";
		String content = "Hallo";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType, "content",
				content);
		slingContext.currentResource(resource);
		slingContext.request().setAttribute(Sling.ATTRIBUTE_AEM_REACT_DIALOG, true);

		String html = render();

		Assert.assertEquals("", html);

	}

	@Test
	public void testRenderCache() throws NoSuchElementException, IllegalStateException, Exception {
		String resourceType = "react-demo/components/text";
		String path = "/content/page/test";
		String content = "Hallo";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType, "content",
				content);
		slingContext.currentResource(resource);
		slingContext.requestPathInfo().setSelectorString("json");

		String json = render();
		JsonNode cache = new ObjectMapper().readTree(json);

		Assert.assertEquals(content, cache.get("transforms").get(path ).get("content").asText());

	}

	private String render() throws Exception, ScriptException {
		Mockito.when(loader.iterator()).thenAnswer((InvocationOnMock mock) -> {
			return createScripts().iterator();
		});

		Mockito.when(requestDispatcherFactory.getRequestDispatcher(Mockito.any(Resource.class),
				Mockito.any(RequestDispatcherOptions.class))).thenReturn(dispatcher);
		Mockito.doAnswer((InvocationOnMock invoke) -> {
			((SlingHttpServletResponse)invoke.getArguments()[1]).getWriter().println("<div><cq data=\"some\"/></div>");
			return null;
		}).when(dispatcher).include(Mockito.any(SlingHttpServletRequest.class),
				Mockito.any(SlingHttpServletResponse.class));
		slingContext.request().setRequestDispatcherFactory(requestDispatcherFactory);
		JavascriptEngine jsEngine = new JavascriptEngine();
		ScriptContext scriptContext = new SimpleScriptContext();
		jsEngine.initialize(loader, new Sling(scriptContext));

		ReactScriptEngine r = new ReactScriptEngine(factory, enginePool, null, dynamicClassLoaderManager, "span",
				"test xxx", modelFactory, null, mapper, new ComponentMetricsService(), false, false, false);
		ClassLoader classLoader = this.getClass().getClassLoader();
		Mockito.when(factory.getClassLoader()).thenReturn(classLoader);
		Mockito.when(dynamicClassLoaderManager.getDynamicClassLoader()).thenReturn(classLoader);

		Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put(SlingBindings.REQUEST, slingContext.request());
		bindings.put(SlingBindings.RESPONSE, slingContext.response());
		bindings.put(SlingBindings.SLING, sling);

		Mockito.when(enginePool.borrowObject()).thenReturn(jsEngine);

		StringWriter writer = new StringWriter();
		scriptContext.setWriter(writer);
		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		return writer.getBuffer().toString();

	}

	private List<HashedScript> createScripts() throws IOException {
		String script1 = IOUtils.toString(getClass().getResource("/com/sinnerschrader/aem/react/nashorn-polyfill.js"));
		String script2 = IOUtils.toString(getClass().getResource("/ts/reactserver.js"));

		List<HashedScript> scripts = new ArrayList<>();
		scripts.add(new HashedScript("ff1", script1, "/script1"));
		scripts.add(new HashedScript("ff2", script2, "/script2"));
		return scripts;
	}

}
