package com.sinnerschrader.aem.react.cache;


import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;

/**
 * A {@link StatsCounter} instrumented with Dropwizard Metrics.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class CacheStatsCounter implements StatsCounter {
  private final Counter hitCount;
  private final Counter missCount;
  private final Counter loadSuccessCount;
  private final Counter loadFailureCount;
  private final Timer totalLoadTime;
  private final Counter evictionCount;
  private final Counter evictionWeight;

  /**
   * Constructs an instance for use by a single cache.
   *
   * @param registry the registry of metric instances
   * @param metricsPrefix the prefix name for the metrics
   */
  public CacheStatsCounter(MetricRegistry registry, String metricsPrefix) {
    requireNonNull(metricsPrefix);
    hitCount = registry.counter(metricsPrefix + ".hits");
    missCount = registry.counter(metricsPrefix + ".misses");
    totalLoadTime = registry.timer(metricsPrefix + ".loads");
    loadSuccessCount = registry.counter(metricsPrefix + ".loads-success");
    loadFailureCount = registry.counter(metricsPrefix + ".loads-failure");
    evictionCount = registry.counter(metricsPrefix + ".evictions");
    evictionWeight = registry.counter(metricsPrefix + ".evictions-weight");
  }

  @Override
  public void recordHits(int count) {
    hitCount.inc(count);
  }

  @Override
  public void recordMisses(int count) {
    missCount.inc(count);
  }

  @Override
  public void recordLoadSuccess(long loadTime) {
    loadSuccessCount.inc();
    totalLoadTime.update(loadTime, TimeUnit.NANOSECONDS);
  }

  @Override
  public void recordLoadFailure(long loadTime) {
    loadFailureCount.inc();
    totalLoadTime.update(loadTime, TimeUnit.NANOSECONDS);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void recordEviction() {
    // This method is scheduled for removal in version 3.0 in favor of recordEviction(weight)
    recordEviction(1);
  }

  @Override
  public void recordEviction(int weight) {
    evictionCount.inc();
    evictionWeight.inc(weight);
  }

  @Override
  public CacheStats snapshot() {
    return new CacheStats(
        hitCount.getCount(),
        missCount.getCount(),
        loadSuccessCount.getCount(),
        loadFailureCount.getCount(),
        totalLoadTime.getCount(),
        evictionCount.getCount(),
        evictionWeight.getCount());
  }

  @Override
  public String toString() {
    return snapshot().toString();
  }
}