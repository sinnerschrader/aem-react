package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.commons.pool2.ObjectPool;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
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

@RunWith(MockitoJUnitRunner.class)
public class ReactScriptEngineTest {

	@Mock
	private Resource resource;

	@Mock
	private ReactScriptEngineFactory factory;

	@Mock
	private ResourceResolver resourceResolver;

	@Mock
	private ClassLoader classLoader;

	@Mock
	private DynamicClassLoaderManager dynamicClassLoaderManager;

	@Mock
	private SlingHttpServletRequest request;

	@Mock
	private SlingHttpServletResponse response;

	@Mock
	private RequestPathInfo info;

	@Mock
	private ObjectPool<JavascriptEngine> enginePool;

	@Mock
	private JavascriptEngine engine;

	@Mock
	private SlingScriptHelper sling;

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

	@Test
	public void testEval() throws NoSuchElementException, IllegalStateException, Exception {
		ReactScriptEngine r = new ReactScriptEngine(factory, enginePool, null, dynamicClassLoaderManager, "span",
				"test xxx", null, null, null);
		Mockito.when(factory.getClassLoader()).thenReturn(classLoader);
		ScriptContext scriptContext = new SimpleScriptContext();
		StringWriter writer = new StringWriter();
		scriptContext.setWriter(writer);
		Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.put(SlingBindings.REQUEST, request);
		bindings.put(SlingBindings.RESPONSE, response);
		bindings.put(SlingBindings.SLING, sling);
		Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);
		Mockito.when(request.getRequestPathInfo()).thenReturn(info);
		Mockito.when(info.getSelectors()).thenReturn(new String[0]);

		Mockito.when(enginePool.borrowObject()).thenReturn(engine);
		RenderResult result = new RenderResult();
		result.cache = "{\"cache\":true}";
		result.html = "<div></div>";

		String resourceType = "/apps/test";
		Mockito.when(resource.getResourceType()).thenReturn(resourceType);
		String path = "/content/page/test";
		Mockito.when(resourceResolver.map(request, path)).thenReturn(path);
		Mockito.when(resource.getPath()).thenReturn(path);
		Mockito.when(request.getResource()).thenReturn(resource);

		Mockito.when(engine.render(Matchers.eq(path), Matchers.eq(resourceType), Matchers.eq("disabled"),
				Mockito.anyObject(), Matchers.eq(false), Matchers.eq(null))).thenReturn(result);
		RenderResult renderResult = (RenderResult) r.eval(new StringReader(""), scriptContext);
		Assert.assertNull(renderResult);
		String renderedHtml = writer.getBuffer().toString();

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
		Assert.assertEquals(path + "_component", wrapper.attr("data-react-id"));
		Assert.assertEquals(path + "_component", textarea.attr("id"));

	}
}
