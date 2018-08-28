package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.api.JsProxy;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;

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

	@Mock
	private ScriptCollectionLoader loader;

	@Mock
	private Sling sling;

	@Test
	public void testNoChanges() {
		JavascriptEngine engine = new JavascriptEngine(loader);

		List<HashedScript> scripts = setupScripts();

		engine.initialize();

		// check that only the checksum is relevant
		scripts.clear();
		HashedScript scriptv2 = new HashedScript("1", "asdasdasdsa", "1");
		scripts.add(scriptv2);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		Assert.assertFalse(engine.isScriptsChanged());

	}

	@Test
	public void testChanges() {
		JavascriptEngine engine = new JavascriptEngine(loader);

		List<HashedScript> scripts = setupScripts();

		engine.initialize();

		// check that checksum means it is changed
		scripts.clear();
		HashedScript scriptv3 = new HashedScript("2", "", "1");
		scripts.add(scriptv3);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		Assert.assertTrue(engine.isScriptsChanged());

	}

	@Test
	public void testNoScriptsChanges() {
		JavascriptEngine engine = new JavascriptEngine(loader);

		List<HashedScript> scripts = setupScripts();

		engine.initialize();

		// check that checksum means it is changed
		scripts.clear();
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		Assert.assertTrue(engine.isScriptsChanged());

	}

	@Test
	public void testMoreScriptsChanges() {
		JavascriptEngine engine = new JavascriptEngine(loader);

		List<HashedScript> scripts = setupScripts();

		engine.initialize();

		// check that checksum means it is changed
		scripts.clear();
		HashedScript scriptv3 = new HashedScript("1", "", "1");
		HashedScript scriptv4 = new HashedScript("2", "", "1");
		scripts.add(scriptv3);
		scripts.add(scriptv4);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		Assert.assertTrue(engine.isScriptsChanged());

	}

	private List<HashedScript> setupScripts() {
		List<HashedScript> scripts = new ArrayList<>();
		HashedScript script = new HashedScript("1", "", "1");
		scripts.add(script);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		return scripts;
	}

	@Test
	@Ignore("fix me")
	public void testRender() throws IOException {
		URL resource = this.getClass().getResource("/react.js");
		String js = IOUtils.toString(resource);
		JavascriptEngine jsEngine = new JavascriptEngine(loader);
		List<HashedScript> scripts = new ArrayList<>();
		HashedScript script = new HashedScript("1", js, "1");
		scripts.add(script);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		jsEngine.initialize();

		ReactRenderEngine renderEngine = new ReactRenderEngine(jsEngine.createBindings());

		List<String> selectors = new ArrayList() {
			{
				this.add("s1");
			}
		};
		RenderResult result = renderEngine.render("/content", "/apps/test", 1, "disabled", new MockCqx(), false,
				selectors);
		Assert.assertEquals("my html", result.html);
		JsonNode tree = new ObjectMapper().readTree(result.cache);
		Assert.assertEquals("/content", tree.get("path").textValue());
		Assert.assertEquals("s1", tree.get("selectors").get(0).textValue());
		Assert.assertEquals(MockCqx.CQX_RESPONSE, tree.get("cqx").textValue());
		//Assert.assertEquals(reactContext, result.reactContext);
	}

	@Test
	public void testConsole() {
		JavascriptEngine.Console console = new JavascriptEngine.Console();
		console.debug("test", null);
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
