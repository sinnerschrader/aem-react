package com.sinnerschrader.aem.react;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PoolManager.class);

	private final JavascriptEngine jsEngine;

	private final LinkedBlockingQueue<ReactRenderEngine> renderer;

	private final int maxRendererSize;

    private final Timer getRendererTimer;
    private final Timer createRendererTimer;
    private final Timer renderTimer;

	private final AtomicInteger rootLevelEngineCount = new AtomicInteger(0);
	private final AtomicInteger nonRootLevelEngineCount = new AtomicInteger(0);

	private String stateHash;

    public PoolManager(int maxRendererSize, ComponentMetricsService metricsService) {
        this.jsEngine = new JavascriptEngine();
		this.maxRendererSize = maxRendererSize;
		this.renderer = new LinkedBlockingQueue<>();
        metricsService.getRegistry().register("react.total.queue.size",
				(Gauge<Integer>) () -> (rootLevelEngineCount.intValue() + nonRootLevelEngineCount.intValue())
		);
        getRendererTimer = metricsService.getRegistry().timer("react.get.renderer.duration");
        createRendererTimer = metricsService.getRegistry().timer("react.create.renderer.duration");
		renderTimer = metricsService.getRegistry().timer("react.render.duration");
    }

	public void updateScripts(List<String> scripts) {
		LOGGER.info("pm: updating scripts");
		jsEngine.compileScript(scripts);
		stateHash = UUID.randomUUID().toString();
		renderer.clear();
		rootLevelEngineCount.set(0);
		nonRootLevelEngineCount.set(0);
	}

	public interface EngineUser<T> {
		T execute(ReactRenderEngine engine) throws Exception;
	}

	public void close() {
		renderer.clear();
	}

	public <T> T execute(EngineUser<T> processor) throws Exception {
		final Timer.Context context = renderTimer.time();
		T result = JsExecutionStack.execute((int level) -> {
			ReactRenderEngine renderEngine = null;
			try {
				renderEngine = getRenderer(level);
				return processor.execute(renderEngine);
			} catch (InterruptedException e) {
				LOGGER.error("unable to get renderer from queue", e);
				throw new TechnicalException("unable to get renderer from queue");
			} finally {
				if (renderEngine != null && renderEngine.isValid(stateHash)) {
					this.renderer.put(renderEngine);
				}
			}
		});

		context.stop();
		return result;
	}

	private ReactRenderEngine getRenderer(int level) throws InterruptedException {
        final Timer.Context fullTime = getRendererTimer.time();
        ReactRenderEngine renderer = this.renderer.poll();
		if (renderer == null) {
            Timer.Context createTime = createRendererTimer.time();
			renderer = tryCreateRenderer(level);
            createTime.stop();
			if (renderer == null) {
                final long start = System.currentTimeMillis();
                renderer = this.renderer.poll(10, TimeUnit.SECONDS);
                LOGGER.debug("pm: waited {}ms to get an render engine from pool", (System.currentTimeMillis() - start));
			}
		}

        fullTime.stop();
		return renderer;
	}

	private synchronized ReactRenderEngine tryCreateRenderer(int level) {
		if (level > 1 || rootLevelEngineCount.get() < maxRendererSize) {
			ReactRenderEngine renderer = new ReactRenderEngine(jsEngine.createBindings(), stateHash);
			if (level == 1) {
				rootLevelEngineCount.incrementAndGet();
			} else {
				nonRootLevelEngineCount.incrementAndGet();
			}
			return renderer;
		}

		return null;
	}
}
