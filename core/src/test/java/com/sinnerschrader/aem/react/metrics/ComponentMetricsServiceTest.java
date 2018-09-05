package com.sinnerschrader.aem.react.metrics;

import java.util.Hashtable;

import org.apache.sling.api.resource.Resource;
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
		Hashtable map = new Hashtable<String, Object>() {
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
		Hashtable map = new Hashtable<String, Object>() {
			{
				this.put("metrics.enabled", true);
				this.put("metrics.jmx.enabled", true);
			}
		};
		context.registerService(ComponentMetricsService.class, new ComponentMetricsService(), map);
		ComponentMetricsService service = context.getService(ComponentMetricsService.class);
		service.start(map);
		service.stop();
	}


	@Test
	public void testEnabled() throws InterruptedException {
		Hashtable map = new Hashtable<String, Object>() {
			{
				this.put("metrics.enabled", true);
			}
		};
		context.registerService(ComponentMetricsService.class, new ComponentMetricsService(), map);
		ComponentMetricsService service = context.getService(ComponentMetricsService.class);
		service.start(map);
		service.stop();
	}

	@Test
	public void testCreateResourceEnabled() throws InterruptedException {
		Hashtable map = new Hashtable<String, Object>() {
			{
				this.put("metrics.enabled", true);
			}
		};
		context.registerService(ComponentMetricsService.class, new ComponentMetricsService(), map);
		ComponentMetricsService service = context.getService(ComponentMetricsService.class);
		service.start(map);

		Resource resource = context.create().resource("/test","sling:resourceType","/apps/test");
		ComponentMetrics metrics = service.create(resource);
	}

	@Test
	public void testCreateResourceDisabled() throws InterruptedException {
		Hashtable map = new Hashtable<String, Object>() {
			{
				this.put("metrics.enabled", false);
			}
		};
		context.registerService(ComponentMetricsService.class, new ComponentMetricsService(), map);
		ComponentMetricsService service = context.getService(ComponentMetricsService.class);
		service.start(map);

		Resource resource = context.create().resource("/test");
		ComponentMetrics metrics = service.create(resource);
	}


}
