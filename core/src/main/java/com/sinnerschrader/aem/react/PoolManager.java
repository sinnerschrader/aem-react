package com.sinnerschrader.aem.react;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;

public class PoolManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PoolManager.class);

	private final JavascriptEngine jsEngine;

	private final LinkedBlockingQueue<ReactRenderEngine> renderer;

	private ThreadLocal<ReactRenderEngine> localRenderer = new ThreadLocal<>();

	private AtomicInteger engineCount = new AtomicInteger(0);

	public interface EngineUser<T> {
		T execute(ReactRenderEngine engine) throws Exception;
	}

	public PoolManager(ScriptCollectionLoader loader, boolean reloadScriptOnChange) {
		this.jsEngine = new JavascriptEngine(loader, reloadScriptOnChange);
		this.renderer = new LinkedBlockingQueue<>();
	}

	public void close() {
		renderer.clear();
	}

	public int getEngineCount() {
		return engineCount.get();
	}

	public <T> T execute(EngineUser<T> processor) throws Exception {
		jsEngine.tryCompileScript();
		return JsExecutionStack.execute((int level) -> {
			ReactRenderEngine renderEngine = localRenderer.get();
			try {
				if (level != 1 && renderEngine == null) {
					throw new IllegalStateException("renderer is null, but is expected to be none null");
				}

				if (jsEngine.isScriptsChanged()) {
					renderer.clear();
					jsEngine.compileScript();
				}

				if (renderEngine == null) {
					renderEngine = getRenderer();
					localRenderer.set(renderEngine);
				}

				return processor.execute(renderEngine);
			} finally {
				if (level == 1 && renderEngine != null) {
					localRenderer.remove();
					this.renderer.put(renderEngine);
				}
			}
		});

	}

	private ReactRenderEngine getRenderer() {
		ReactRenderEngine renderer = this.renderer.poll();
		if (renderer == null) {
			renderer = new ReactRenderEngine(jsEngine.createBindings());
			engineCount.incrementAndGet();
			LOGGER.debug("pm: created new rre. Total is now: {}", engineCount.get());
		}
		return renderer;
	}
}
