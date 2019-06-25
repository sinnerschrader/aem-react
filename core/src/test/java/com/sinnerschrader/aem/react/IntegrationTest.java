package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.RequestDispatcher;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.cache.ComponentCache;
import com.sinnerschrader.aem.react.integration.TextProps;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationTest {

	@Mock
	private ReactScriptEngineFactory factory;
	@Mock
	private DynamicClassLoaderManager dynamicClassLoaderManager;

	@Mock
	private MockRequestDispatcherFactory requestDispatcherFactory;

	@Mock
	private ObjectPool<JavascriptEngine> enginePool;

	@Mock
	private SlingScriptHelper sling;

	@Mock
	private ModelFactory modelFactory;

	@Mock
	private ComponentMetricsService metricsService;

	private ComponentCache mockCache = new ComponentCache(null, null, 0, 0, null, false);

	private ObjectMapper mapper = new ObjectMapper();

	private ObjectNode getJsonFromScript(Element script) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return (ObjectNode) objectMapper.readTree(script.html());
	}

	private Element getWrapper(Document doc) {
		Elements es = doc.select("[data-react=\"app\"]");
		return es.get(0);
	}

	private Element getScript(Document doc) {
		Elements es = doc.select("script");
		assertThat(es).hasSize(1);
		return es.get(0);
	}

	@Rule
	public SlingContext slingContext = new SlingContext();

	@Mock
	private RequestDispatcher dispatcher;

	@Before
	public void before() {
		MetricRegistry ms = Mockito.mock(MetricRegistry.class);
		Timer timer = Mockito.mock(Timer.class);
		Timer.Context context = Mockito.mock(Timer.Context.class);
		Mockito.when(timer.time()).thenReturn(context);
		Mockito.when(ms.timer(Mockito.any(String.class))).thenReturn(timer);
		Mockito.when(metricsService.getRegistry()).thenReturn(ms);
	}

	@Test
	public void testRenderText() throws Exception {
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

		Element script = getScript(doc);
		ObjectNode jsonFromScript = getJsonFromScript(script);
		Assert.assertEquals("<span data-reactroot=\"\">Hallo</span>", wrapper.html());
		Assert.assertEquals(resourceType, jsonFromScript.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromScript.get("path").asText());
		Assert.assertEquals(content, jsonFromScript.get("cache").get("transforms").get(path).get("content").asText());
	}

	@Test
	public void testRenderTextRequestModel() throws Exception {
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

		Element script = getScript(doc);
		ObjectNode jsonFromScript = getJsonFromScript(script);
		Assert.assertEquals("<span data-reactroot=\"\">RequestModel</span>", wrapper.html());
		Assert.assertEquals(resourceType, jsonFromScript.get("resourceType").asText());
		Assert.assertEquals(path, jsonFromScript.get("path").asText());
		Assert.assertEquals("RequestModel",
				jsonFromScript.get("cache").get("transforms").get(path + ":requestmodel").get("content").asText());
	}

	@Test
	public void testDialog() throws Exception {
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
	public void testRenderCache() throws Exception {
		String resourceType = "react-demo/components/text";
		String path = "/content/page/test";
		String content = "Hallo";

		Resource resource = slingContext.create().resource(path, "sling:resourceType", resourceType, "content",
				content);
		slingContext.currentResource(resource);
		slingContext.requestPathInfo().setSelectorString("json");

		String json = render();
		JsonNode cache = new ObjectMapper().readTree(json);

		Assert.assertEquals(content, cache.get("transforms").get(path).get("content").asText());

	}

	private String render() throws Exception {
		Mockito.when(requestDispatcherFactory.getRequestDispatcher(Mockito.any(Resource.class),
				Mockito.any(RequestDispatcherOptions.class))).thenReturn(dispatcher);
		Mockito.doAnswer((InvocationOnMock invoke) -> {
			((SlingHttpServletResponse) invoke.getArguments()[1]).getWriter().println("<div><cq data=\"some\"/></div>");
			return null;
		}).when(dispatcher).include(Mockito.any(SlingHttpServletRequest.class),
				Mockito.any(SlingHttpServletResponse.class));
		slingContext.request().setRequestDispatcherFactory(requestDispatcherFactory);
		ScriptContext scriptContext = new SimpleScriptContext();
		JavascriptEngine jsEngine = new JavascriptEngine();

		PoolManager poolManager = new PoolManager(5, metricsService);
		poolManager.updateScripts(createScripts());

		ReactScriptEngine r = new ReactScriptEngine(factory, poolManager,null, dynamicClassLoaderManager,
				"span", "test xxx", modelFactory, null, mapper, new ComponentMetricsService(), false, false, false,
				mockCache);
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

	private List<String> createScripts() throws IOException {
		String script1 = IOUtils.toString(getClass().getResource("/com/sinnerschrader/aem/react/nashorn-polyfill.js"));
		String script2 = IOUtils.toString(getClass().getResource("/ts/reactserver.js"));

		List<String> scripts = new ArrayList<>();
		scripts.add(script1);
		scripts.add(script2);
		return scripts;
	}

}
