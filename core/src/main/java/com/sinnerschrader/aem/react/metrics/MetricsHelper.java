package com.sinnerschrader.aem.react.metrics;

public class MetricsHelper {
	public static ComponentMetrics getCurrent() {
		ComponentMetrics current = DefaultComponentMetrics.getCurrent();
		if (current == null) {
			return ComponentMetricsService.NO_OP;
		}
		return current;
	}
}
