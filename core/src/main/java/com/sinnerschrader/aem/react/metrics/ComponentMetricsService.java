package com.sinnerschrader.aem.react.metrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;

@Service(ComponentMetricsService.class)
@Component(immediate = true, metatype = true)
public class ComponentMetricsService {

	private static final String METRICS_ENABLED = "metrics.enabled";

	private static final String METRICS_JMX_ENABLED = "metrics.jmx.enabled";

	private static final String METRICS_REPORTING_RATE = "metrics.reporting.rate";

	static final ComponentMetrics NO_OP = new DummyComponentMetrics();

	private MetricRegistry metricRegistry;

	@Property(name = METRICS_ENABLED, label = "enabled", boolValue = false)
	private boolean enabled;

	@Property(name = METRICS_JMX_ENABLED, label = "jmx enabled", boolValue = false)
	private boolean jmxEnabled;

	@Property(name = METRICS_REPORTING_RATE, label = "reporting rate", longValue = 5)
	private long reportingRate;

	private ScheduledReporter logReporter;

	private JmxReporter jmxReporter;

	public ComponentMetrics create(Resource resource) {
		if (enabled) {
			return DefaultComponentMetrics.create(resource, metricRegistry);
		}
		return NO_OP;

	}


	@Activate
	public void start(Map<String, Object> dictionary) {
		configure(dictionary);
	}

	private void configure(Map<String, Object> dictionary) {
		this.reportingRate = PropertiesUtil.toLong(dictionary.get(METRICS_REPORTING_RATE), 5l);
		this.enabled = PropertiesUtil.toBoolean(dictionary.get(METRICS_ENABLED), false);
		this.jmxEnabled = PropertiesUtil.toBoolean(dictionary.get(METRICS_JMX_ENABLED), false);
		if (enabled) {
			Logger logger = LoggerFactory.getLogger(ComponentMetricsService.class);
			metricRegistry = new MetricRegistry();
			if (jmxEnabled) {
				jmxReporter = JmxReporter.forRegistry(metricRegistry)//
						.convertRatesTo(TimeUnit.SECONDS)//
						.convertDurationsTo(TimeUnit.MILLISECONDS)//
						.build();
				jmxReporter.start();

			} else {
				logReporter = Slf4jReporter.forRegistry(metricRegistry)//
						.outputTo(logger)//
						.convertRatesTo(TimeUnit.SECONDS)//
						.convertDurationsTo(TimeUnit.MILLISECONDS)//
						.build();
				logReporter.start(reportingRate, TimeUnit.MINUTES);
			}

		}

	}

	@Deactivate
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

	@Modified
	public void modified(Map<String, Object> dictionary) {
		this.stop();
		this.start(dictionary);
	}
}
