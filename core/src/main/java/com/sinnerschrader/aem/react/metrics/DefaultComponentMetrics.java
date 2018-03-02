package com.sinnerschrader.aem.react.metrics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.sling.api.resource.Resource;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class DefaultComponentMetrics implements AutoCloseable, ComponentMetrics {

	private static final ThreadLocal<LinkedList<ComponentMetrics>> metrics = new ThreadLocal<LinkedList<ComponentMetrics>>() {

		@Override
		protected LinkedList<ComponentMetrics> initialValue() {
			return new LinkedList<>();
		}

	};

	private String rootName;
	private MetricRegistry metricRegistry;
	private Map<String, Context> timers = new HashMap<>();

	public DefaultComponentMetrics(String rootName, MetricRegistry metrics) {
		super();
		this.rootName = rootName;
		this.metricRegistry = metrics;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.sinnerschrader.aem.react.ComponentMetrics#timer(java.lang.String)
	 */
	@Override
	public void timer(String name) {
		String fullname = getName(name);
		Timer timer = metricRegistry.timer(fullname);

		Context ctx = timer.time();
		timers.put(fullname, ctx);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.sinnerschrader.aem.react.ComponentMetrics#timer(java.lang.String,
	 * java.util.concurrent.Callable)
	 */
	@Override
	public <T> T timer(String name, Callable<T> callable) {
		String fullname = getName(name);
		Timer timer = metricRegistry.timer(fullname);

		try {
			return timer.time(callable);
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception during timer invocation", e);
		}

	}

	private String getName(String name) {
		StringBuilder builder = new StringBuilder(rootName);
		builder.append(".");
		builder.append(name);
		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.sinnerschrader.aem.react.ComponentMetrics#timerEnd(java.lang.String)
	 */
	@Override
	public void timerEnd(String name) {
		String fullname = getName(name);
		Context timer = timers.get(fullname);
		if (timer != null) {
			timer.stop();
			timers.remove(fullname);
		}

	}

	@Override
	public void close() throws Exception {
		timerEnd("total");
		metrics.get().remove(this);
	}

	public static ComponentMetrics create(Resource resource, MetricRegistry metricRegistry) {

		String resourceType = resource.getResourceType();
		if (resourceType.endsWith("/")) {
			resourceType = resourceType.substring(0, resourceType.length() - 1);
		}
		int lastIndex = resourceType.lastIndexOf('/');
		String rootname = resourceType.substring(lastIndex + 1, resourceType.length());
		ComponentMetrics componentMetrics = new DefaultComponentMetrics(rootname, metricRegistry);
		metrics.get().add(componentMetrics);

		componentMetrics.timer("total");

		return componentMetrics;
	}

	public static ComponentMetrics getCurrent() {
		LinkedList<ComponentMetrics> current = metrics.get();
		if (current == null || current.isEmpty()) {
			return null;
		}
		return current.getLast();
	}

}
