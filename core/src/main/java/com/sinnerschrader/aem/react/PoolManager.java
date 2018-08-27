package com.sinnerschrader.aem.react;

import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PoolManager.class);

	private ScriptCollectionLoader loader;

	private LinkedBlockingQueue<JavascriptEngine> engines;

	private ThreadLocal<JavascriptEngine> localEngine = new ThreadLocal<>();

	private AtomicInteger engineCount = new AtomicInteger(0);

	public interface EngineUser<T> {
		T execute(JavascriptEngine engine) throws Exception;
	}

	public PoolManager(ScriptCollectionLoader loader) {
		this.loader = loader;
		this.engines = new LinkedBlockingQueue<>();
	}

	public void close() {
		engines.clear();
	}

	public int getEngineCount() {
		return engineCount.get();
	}

	public <T> T execute(EngineUser<T> processor) throws Exception {
		return JsExecutionStack.execute((int level) -> {
			JavascriptEngine engine = localEngine.get();
			try {
				if (level != 1 && engine == null) {
					throw new IllegalStateException("engine is null, but is expected to be none null");
				}

				if (engine == null) {
					engine = getEngine();
					localEngine.set(engine);
				}

				if (engine.isScriptsChanged()) {
					engine.initialize(true);
				}

				return processor.execute(engine);
			} finally {
				if (level == 1 && engine != null) {
					localEngine.remove();
					engines.put(engine);
				}
			}
		});
	}

	private JavascriptEngine getEngine() {
		JavascriptEngine engine = engines.poll();
		if (engine == null) {
			engine =  new JavascriptEngine(loader);
			engine.initialize(false);
			engineCount.incrementAndGet();
			LOGGER.debug("pm: created new jse. Total is now: " + engineCount.get());
		}
		return engine;
	}
}
