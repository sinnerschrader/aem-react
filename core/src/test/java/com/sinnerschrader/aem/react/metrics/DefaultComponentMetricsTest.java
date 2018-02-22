package com.sinnerschrader.aem.react.metrics;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

public class DefaultComponentMetricsTest {

	@Rule
	public SlingContext context= new SlingContext();

	@Test
	public void test() throws Exception {
		MetricRegistry metricRegistry = new MetricRegistry();
		try (DefaultComponentMetrics c = new DefaultComponentMetrics("", metricRegistry)) {
			c.timer("test");
			c.timerEnd("test");
		}
	}

	@Test
	public void testCallable() throws Exception {
		MetricRegistry metricRegistry = new MetricRegistry();
		try (DefaultComponentMetrics c = new DefaultComponentMetrics("", metricRegistry)) {
			c.timer("test", ()-> {return null;});
		}
	}

	@Test
	public void testTotal() throws Exception {

		Resource resource = context.create().resource("/test","sling:resourceType","/apps/test");
		try (ComponentMetrics c = DefaultComponentMetrics.create(resource, new MetricRegistry())) {
		}
	}

}
