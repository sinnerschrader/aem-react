package com.sinnerschrader.aem.react.cache;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.sinnerschrader.aem.react.ReactScriptEngine;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;

public class ComponentCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCache.class);

	public interface ResultRenderer {
		RenderResult render() throws Exception;
	}

	private Cache<CacheKey, CachedHtml> cache;

	private ModelFactory modelFactory;

	private boolean caching;

	private Counter cacheErrorCounter;

	private Counter cacheHitCounter;

	private Counter cacheTotalCounter;

	private Counter cacheModelMissCounter;

	private Counter cacheHtmlMissCounter;

	private Counter cacheRenderResultLengths;

	private boolean debug;

	public ComponentCache(ModelFactory modelFactory, ObjectWriter mapper, int maxSize, int maxMinutes,
			ComponentMetricsService metricsService, boolean debug) {
		super();
		this.debug = debug;
		if (metricsService != null && metricsService.getRegistry() != null) {
			MetricRegistry metricRegistry = metricsService.getRegistry();
			this.cacheErrorCounter = metricRegistry.counter("react.cache.error.total");
			this.cacheHitCounter = metricRegistry.counter("react.cache.hit.total");
			this.cacheTotalCounter = metricRegistry.counter("react.cache.total");
			this.cacheModelMissCounter = metricRegistry.counter("react.cache.misses.models.total");
			this.cacheHtmlMissCounter = metricRegistry.counter("react.cache.misses.html.total");
			this.cacheRenderResultLengths = metricRegistry.counter("react.cache.render_results.total_length");
			metricRegistry.gauge("react.cache.hitrate", () -> new RatioGauge() {
				@Override
				protected Ratio getRatio() {
					return Ratio.of(cacheHitCounter.getCount(), cacheTotalCounter.getCount());
				}
			});
			metricRegistry.gauge("react.cache.missrate", () -> new RatioGauge() {
				@Override
				public Ratio getRatio() {
					long misses = cacheModelMissCounter.getCount() + cacheHtmlMissCounter.getCount() + cacheErrorCounter.getCount();
					return Ratio.of(misses, cacheTotalCounter.getCount());
				}
			});
		}
		if (maxSize <= 0) {
			caching = false;
		} else {
			caching = true;
			this.modelFactory = modelFactory;
			this.mapper = mapper;
			Caffeine<CacheKey, CachedHtml> builder = Caffeine.newBuilder()//
					.expireAfterWrite(maxMinutes, TimeUnit.MINUTES)//
					.maximumSize(maxSize)
					.removalListener((CacheKey key, CachedHtml value, RemovalCause cause) -> {
						LOGGER.debug("Key {} was removed ({})", key, cause);
						if (value == null || cacheRenderResultLengths == null) {
							return;
						}
						long removedSize = sumRenderResultSizes(value.getRenderResult());
						cacheRenderResultLengths.dec(removedSize);
					})
					;

			if (metricsService != null && metricsService.getRegistry() != null) {
				builder.recordStats(metricsService::getCacheStatsCounter);
			}
			cache = builder.build();
		}
	}

	private ObjectWriter mapper;

	private Object getModel(SlingHttpServletRequest request) {
		try {
			return modelFactory.getModelFromRequest(request);
		} catch (ModelClassException e) {
			return null;
		}
	}

	private String generateChecksum(String model) {
		if (debug) {
			return model;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(model.getBytes(StandardCharsets.UTF_8));
			byte[] hash = digest.digest();
			return new String(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("hashing algorithm not available", e);
		}
	}

	private CachedHtml getHtml(CacheKey cacheKey, Object cacheableModel) {
		CachedHtml cachedHtml = cache.getIfPresent(cacheKey);
		if (cachedHtml == null) {
			return null;
		}
		String checksum = generateChecksum(cacheableModel);
		if (checksum == null) {
			return null;
		}
		if (checksum.equals(cachedHtml.getModel())) {
			return cachedHtml;

		}
		if (debug) {
			LOGGER.debug("diff model: {}", StringUtils.difference(cachedHtml.getModel(), checksum));
		}

		return null;
	}

	private String generateChecksum(Object cacheableModel) {
		StringWriter writer = new StringWriter();
		try {
			mapper.writeValue(writer, cacheableModel);
		} catch (IOException e) {
			LOGGER.error("cannot generate checksum because model cannot be serialized to json", e);
			return null;
		}
		return generateChecksum(writer.toString());
	}

	public void put(CacheKey cacheKey, Object cacheableModel, RenderResult result, Integer rootNo) {
		String checksum = generateChecksum(cacheableModel);
		if (checksum == null) {
			return;
		}
		cache.put(cacheKey, new CachedHtml(checksum, result, rootNo));
		if (cacheRenderResultLengths != null) {
			long increasedSize = sumRenderResultSizes(result);
			cacheRenderResultLengths.inc(increasedSize);
		}
	}

	public boolean isCaching() {
		return caching;
	}

	public RenderResult cache(ModelCollector collector, CacheKey key, SlingHttpServletRequest request, String path, String resourceType,
			ResultRenderer render) throws Exception {
		if (caching) {
			incCounter(cacheTotalCounter);
			try {
				Object cacheableModel = getModel(request);
				if (cacheableModel != null) {
					collector.addRequestModel(path, cacheableModel);
					CachedHtml cachedHtml = getHtml(key, cacheableModel);
					if (cachedHtml != null) {
						RenderResult result = cachedHtml.getRenderResult();
						request.setAttribute(ReactScriptEngine.REACT_ROOT_NO_KEY, cachedHtml.getRootNo());
						LOGGER.debug("returning cache html for {}", path);
						incCounter(cacheHitCounter);
						return result;
					}

					incCounter(cacheHtmlMissCounter);
					RenderResult result = render.render();
					Integer rootNo = (Integer) request.getAttribute(ReactScriptEngine.REACT_ROOT_NO_KEY);
					put(key, cacheableModel, result, rootNo);
					return result;
				}

				LOGGER.debug("no model for {} and type {}", path, resourceType);
				cacheModelMissCounter.inc();
			} catch (Exception e) {
				incCounter(cacheErrorCounter);
				LOGGER.error("cannot create model for {}. {}", path, e.getMessage());
			}
		}

		return render.render();
	}

	public void clear() {
		if (caching && cache != null) {
			this.cache.invalidateAll();
		}
	}

	private void incCounter(Counter counter) {
		if (counter != null) {
			counter.inc();
		}
	}

	private static long sumRenderResultSizes(RenderResult renderResult) {
		if (renderResult == null) {
			return 0;
		}
		long sum = 0;
		if (renderResult.cache != null) {
			sum += renderResult.cache.length();
		}
		if (renderResult.html != null) {
			sum += renderResult.html.length();
		}
		return sum;
	}
}
