package com.sinnerschrader.aem.react.metrics;

import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.sinnerschrader.aem.react.cache.CacheStatsCounter;

public class ComponentMetricsService {

	public  static final String METRICS_ENABLED = "metrics.enabled";

	public static final String METRICS_JMX_ENABLED = "metrics.jmx.enabled";

	public static final String METRICS_REPORTING_RATE = "metrics.reporting.rate";

	static final ComponentMetrics NO_OP = new DummyComponentMetrics();

	private MetricRegistry metricRegistry;

	private boolean logEnabled;
	private boolean jmxEnabled;

	private long reportingRate;

	private ScheduledReporter logReporter;

	private JmxReporter jmxReporter;

	public ComponentMetrics create(Resource resource) {
		if (!logEnabled && !jmxEnabled) {
			return NO_OP;
		}
		return DefaultComponentMetrics.create(resource, metricRegistry);

	}

	public void start(Dictionary<String, Object> dictionary) {
		configure(dictionary);
	}

	private void configure(Dictionary<String, Object> dictionary) {
		this.reportingRate = PropertiesUtil.toLong(dictionary.get(METRICS_REPORTING_RATE), 5l);
		this.logEnabled = PropertiesUtil.toBoolean(dictionary.get(METRICS_ENABLED), false);
		this.jmxEnabled = PropertiesUtil.toBoolean(dictionary.get(METRICS_JMX_ENABLED), false);
		metricRegistry = new MetricRegistry();
		Logger logger = LoggerFactory.getLogger(ComponentMetricsService.class);
		if (jmxEnabled) {
			jmxReporter = JmxReporter.forRegistry(metricRegistry)//
					.convertRatesTo(TimeUnit.SECONDS)//
					.convertDurationsTo(TimeUnit.MILLISECONDS)//
					.build();
			jmxReporter.start();

		}
		if (logEnabled) {
			logReporter = Slf4jReporter.forRegistry(metricRegistry)//
					.outputTo(logger)//
					.convertRatesTo(TimeUnit.SECONDS)//
					.convertDurationsTo(TimeUnit.MILLISECONDS)//
					.build();
			logReporter.start(reportingRate, TimeUnit.MINUTES);
		}

	}

	public void stop() {
		metricRegistry = null;
		if (logReporter != null) {
			logReporter.stop();
			logReporter.close();
		}
		if (jmxReporter != null) {
			jmxReporter.stop();
			jmxReporter.close();
		}
	}


	public MetricRegistry getRegistry() {
		return metricRegistry;
	}

	public CacheStatsCounter getCacheStatsCounter() {
		return new CacheStatsCounter(metricRegistry, "react.cache");
	}

}
