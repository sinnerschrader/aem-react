package com.sinnerschrader.aem.react.cache;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.RatioGauge;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sinnerschrader.aem.react.ReactScriptEngine;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;

public class ComponentCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCache.class);

	public static interface ResultRenderer {
		public RenderResult render();
	}

	Cache<CacheKey, CachedHtml> cache;

	private ModelFactory modelFactory;

	private boolean caching;

	private Counter cacheHitCounter;

	private Counter cacheTotalCounter;

	private boolean debug;

	public ComponentCache(ModelFactory modelFactory, ObjectWriter mapper, int maxSize, int maxMinutes,
			ComponentMetricsService metricsService, boolean debug) {
		super();
		this.debug = debug;
		if (metricsService != null && metricsService.getRegistry() != null) {
			this.cacheHitCounter = metricsService.getRegistry().counter("react.cache.hit");
			this.cacheTotalCounter = metricsService.getRegistry().counter("react.cache.total");
			metricsService.getRegistry().gauge("react.cache.hitrate", new MetricSupplier<Gauge>() {

				@Override
				public Gauge newMetric() {
					return new RatioGauge() {

						@Override
						protected Ratio getRatio() {
							return Ratio.of(cacheHitCounter.getCount(), cacheTotalCounter.getCount());
						}
					};
				}
			});
		}
		if (maxSize <= 0) {
			caching = false;
		} else {
			caching = true;
			this.modelFactory = modelFactory;
			this.mapper = mapper;
			Caffeine<Object, Object> builder = Caffeine.newBuilder()//
					.expireAfterWrite(maxMinutes, TimeUnit.MINUTES)//
					.maximumSize(maxSize);

			if (metricsService != null && metricsService.getRegistry() != null) {
				builder.recordStats(() -> {
					return metricsService.getCacheStatsCounter();
				});

			}
			cache = builder.build();
		}
	}

	private ObjectWriter mapper;

	public Object getModel(SlingHttpServletRequest request, String path, String resourceType) {
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
			digest.update(model.getBytes("UTF-8"));
			byte[] hash = digest.digest();
			return new String(hash);
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("hashing algorithm not available", e);
		}
	}

	public CachedHtml getHtml(CacheKey cacheKey, Object cacheableModel) {
		if (cacheTotalCounter != null) {
			cacheTotalCounter.inc();
		}
		CachedHtml cachedHtml = cache.getIfPresent(cacheKey);
		if (cachedHtml == null) {
			return null;
		}
		String checksum = generateChecksum(cacheableModel);
		if (checksum == null) {
			return null;
		}
		if (checksum.equals(cachedHtml.getModel())) {
			if (cacheHitCounter != null) {
				cacheHitCounter.inc();
			}
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

	}

	public boolean isCaching() {
		return caching;
	}

	public RenderResult cache(CacheKey key, SlingHttpServletRequest request, String path, String resourceType,
			ResultRenderer render) {
		Object cacheableModel = null;
		if (caching) {

			try {
				cacheableModel = getModel(request, path, resourceType);
				if (cacheableModel != null) {
					CachedHtml cachedHtml = getHtml(key, cacheableModel);
					if (cachedHtml != null) {

						RenderResult result = cachedHtml.getRenderResult();
						request.setAttribute(ReactScriptEngine.REACT_ROOT_NO_KEY, cachedHtml.getRootNo());
						LOGGER.debug("returning cache html for {}", path);
						return result;

					}
					RenderResult result = render.render();
					Integer rootNo = (Integer) request.getAttribute(ReactScriptEngine.REACT_ROOT_NO_KEY);
					put(key, cacheableModel, result, rootNo);
					// LOGGER.debug("no html cached for {}", path);
					return result;
				}
				LOGGER.debug("no model for {}", path);

			} catch (Exception e) {
				LOGGER.error("cannot create model for {}. {}", path, e.getMessage());
			}

		}

		return render.render();

	}

}
