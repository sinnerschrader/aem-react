package com.sinnerschrader.aem.react.metrics;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ComponentMetricsServiceTest {

	@Rule
	public SlingContext context = new SlingContext();

	@Test
	public void testDisabled() throws InterruptedException {
		Map map = new HashMap<String, Object>() {
			{
				this.put("metrics.enabled", false);
			}
		};
		context.registerService(ComponentMetricsService.class, new ComponentMetricsService(), map);
		ComponentMetricsService service = context.getService(ComponentMetricsService.class);
		service.start(map);
		service.stop();
	}


	@Test
	public void testJmxEnabled() throws InterruptedException {
		Map map = new HashMap<String, Object>() {
			{
				this.put("metrics.enabled", true);
				this.put("metrics.jmx.enabled", false);
			}
		};
		context.registerService(ComponentMetricsService.class, new ComponentMetricsService(), map);
		ComponentMetricsService service = context.getService(ComponentMetricsService.class);
		service.start(map);
		service.stop();
	}


	@Test
	public void testEnabled() throws InterruptedException {
		Map map = new HashMap<String, Object>() {
			{
				this.put("metrics.enabled", true);
			}
		};
		context.registerService(ComponentMetricsService.class, new ComponentMetricsService(), map);
		ComponentMetricsService service = context.getService(ComponentMetricsService.class);
		service.start(map);
		service.stop();
	}


}
