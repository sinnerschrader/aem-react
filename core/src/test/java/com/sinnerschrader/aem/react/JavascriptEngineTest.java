package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Cqx;
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

		public String doit() {
			return CQX_RESPONSE;
		}
	}

	@Mock
	private ScriptCollectionLoader loader;

	@Mock
	private Sling sling;

	@Test
	public void testNoChanges() {
		JavascriptEngine engine = new JavascriptEngine();

		List<HashedScript> scripts = setupScripts();

		engine.initialize(loader, sling);

		// check that only the checksum is relevant
		scripts.clear();
		HashedScript scriptv2 = new HashedScript("1", "asdasdasdsa", "1");
		scripts.add(scriptv2);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		Assert.assertFalse(engine.isScriptsChanged());

	}

	@Test
	public void testChanges() {
		JavascriptEngine engine = new JavascriptEngine();

		List<HashedScript> scripts = setupScripts();

		engine.initialize(loader, sling);

		// check that checksum means it is changed
		scripts.clear();
		HashedScript scriptv3 = new HashedScript("2", "", "1");
		scripts.add(scriptv3);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		Assert.assertTrue(engine.isScriptsChanged());

	}

	@Test
	public void testNoScriptsChanges() {
		JavascriptEngine engine = new JavascriptEngine();

		List<HashedScript> scripts = setupScripts();

		engine.initialize(loader, sling);

		// check that checksum means it is changed
		scripts.clear();
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		Assert.assertTrue(engine.isScriptsChanged());

	}

	@Test
	public void testMoreScriptsChanges() {
		JavascriptEngine engine = new JavascriptEngine();

		List<HashedScript> scripts = setupScripts();

		engine.initialize(loader, sling);

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
	public void testRender() throws IOException {
		URL resource = this.getClass().getResource("/react.js");
		String js = IOUtils.toString(resource);
		JavascriptEngine engine = new JavascriptEngine();
		List<HashedScript> scripts = new ArrayList<>();
		HashedScript script = new HashedScript("1", js, "1");
		scripts.add(script);
		Mockito.when(loader.iterator()).thenReturn(scripts.iterator());
		engine.initialize(loader, sling);

		List<String> selectors = new ArrayList() {
			{
				this.add("s1");
			}
		};
		Object reactContext = new Object();
		RenderResult result = engine.render("/content", "/apps/test", "disabled", new MockCqx(), false, reactContext,
				selectors);
		Assert.assertEquals("my html", result.html);
		JsonNode tree = new ObjectMapper().readTree(result.cache);
		Assert.assertEquals("/content", tree.get("path").textValue());
		Assert.assertEquals("s1", tree.get("selectors").get(0).textValue());
		Assert.assertEquals(MockCqx.CQX_RESPONSE, tree.get("cqx").textValue());
		Assert.assertEquals(reactContext, result.reactContext);
	}

}
