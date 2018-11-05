package com.sinnerschrader.aem.react.cache;

import java.util.Collections;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.cache.ComponentCache.ResultRenderer;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;

@RunWith(MockitoJUnitRunner.class)
public class ComponentCacheTest {

	private static final String HTML = "<html>";
	@Mock
	private ComponentMetricsService metricsService;
	@Mock
	private ModelFactory modelFactory;
	@Mock
	private ObjectWriter mapper;
	@Mock
	private SlingHttpServletRequest request;
	@Mock
	private ResultRenderer renderer;


	@Before
	public void before() {
		Mockito.when(modelFactory.getModelFromRequest(request)).thenReturn(new Object());
	}

	@Test
	public void testCacheHit() throws Exception {
		ComponentCache cache = new ComponentCache(modelFactory, mapper, 5, 5, metricsService, false);

		String path = "/content";
		String type = "/apps/m100";

		CacheKey key = new CacheKey(path, type, "disabled", true, Collections.emptyList());
		Integer rootNo = 1;
		RenderResult result = new RenderResult();
		result.html = HTML;
		cache.put(key, new Object(), result, rootNo);

		ModelCollector collector = new ModelCollector();
		RenderResult theResult = cache.cache(collector, key, request, path, type, renderer);

		Assert.assertEquals(theResult.html, HTML);
		Object model = collector.getModel(path, "java.lang.Object");
		Assert.assertNotNull(model);
		Object noModel = collector.getModel(path, "java.lang.ObjectXXX");
		Assert.assertNull(noModel);
	}


	@Test
	public void testNoHit() throws Exception {
		ComponentCache cache = new ComponentCache(modelFactory, mapper, 5, 5, metricsService, false);

		String path = "/content";
		String type = "/apps/m100";

		CacheKey key = new CacheKey(path, type, "disabled", true, Collections.emptyList());
		Integer rootNo = 1;
		RenderResult result = new RenderResult();
		result.html = HTML;

		RenderResult newResult = new RenderResult();
		newResult.html = HTML;

		Mockito.when(renderer.render()).thenReturn(newResult);

		RenderResult theResult = cache.cache(new ModelCollector(), key, request, path, type, renderer);

		Assert.assertEquals(theResult.html, HTML);
	}

}
