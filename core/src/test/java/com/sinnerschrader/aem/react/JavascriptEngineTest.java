package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.sinnerschrader.aem.react.exception.TechnicalException;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.api.JsProxy;

@RunWith(MockitoJUnitRunner.class)
public class JavascriptEngineTest {

	public static class MockCqx extends Cqx {
		private static final String CQX_RESPONSE = "hi";

		public MockCqx() {
			super(null, null, null, null, null);
		}

		public JsProxy getRequestModel(String path, String resourceType) {
			return new JsProxy(CQX_RESPONSE, String.class, new ObjectMapper());
		}
	}

	@Test(expected = TechnicalException.class)
	public void testCompileScriptIsNull() {
		JavascriptEngine engine = new JavascriptEngine();
		engine.createBindings();
	}

	@Test(expected = TechnicalException.class)
	public void testScriptIsNotCompilable() {
		JavascriptEngine engine = new JavascriptEngine();
		engine.compileScript(Collections.singletonList("function broken script;"));
	}

	@Test
	public void testRender() throws IOException {
		URL resource = this.getClass().getResource("/react.js");
		String js = IOUtils.toString(resource);

		JavascriptEngine engine = new JavascriptEngine();
		engine.compileScript(Collections.singletonList(js));

		ReactRenderEngine renderEngine = new ReactRenderEngine(engine.createBindings(), "abc");

		List<String> selectors = Collections.singletonList("s1");
		RenderResult result = renderEngine.render("/content", "/apps/test", 1, "disabled", new MockCqx(), false,
				selectors);
		Assert.assertEquals("my html", result.html);
		JsonNode tree = new ObjectMapper().readTree(result.cache);
		Assert.assertEquals("/content", tree.get("path").textValue());
		Assert.assertEquals("s1", tree.get("selectors").get(0).textValue());
		Assert.assertEquals(MockCqx.CQX_RESPONSE, tree.get("cqx").textValue());
	}

	@Test
	public void testConsole() {
		JavascriptEngine.Console console = new JavascriptEngine.Console();
		console.debug("test");
		console.debug("test", "","");
		console.info("test", null);
		console.info("test");
		console.error("test", null);
		console.error("test");
		console.log("test", null);
		console.log("test");
		console.warn("test", null);
		console.warn("test");
		console.time("test");
		console.timeEnd("test");
	}

}
