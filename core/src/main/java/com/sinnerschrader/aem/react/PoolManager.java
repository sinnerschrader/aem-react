package com.sinnerschrader.aem.react;

import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PoolManager.class);

	public interface EngineUser<T> {
		public T execute(JavascriptEngine engine) throws Exception;
	}

	private ObjectPool<JavascriptEngine> rootPool;
	private ObjectPool<JavascriptEngine> secondLevelpool;

	public PoolManager(ObjectPool<JavascriptEngine> rootPool, ObjectPool<JavascriptEngine> pool) {
		super();
		this.rootPool = rootPool;
		this.secondLevelpool = pool;
	}

	private ObjectPool<JavascriptEngine> get(int level) {
		return level <= 1 ? rootPool : secondLevelpool;
	}

	public void close() {
		rootPool.close();
		secondLevelpool.close();
	}

	public <T> T execute(EngineUser<T> processor) throws Exception {
		return JsExecutionStack.execute((int level) -> {
			ObjectPool<JavascriptEngine> pool = get(level);
			JavascriptEngine engine = pool.borrowObject();
			try {
				engine.initialize();

				while (engine.isScriptsChanged()) {
					LOGGER.info("scripts changed -> invalidate engine");
					pool.invalidateObject(engine);
					engine = pool.borrowObject();
					engine.initialize();
				}
				return processor.execute(engine);

			} finally {
				try {
					pool.returnObject(engine);
				} catch (IllegalStateException e) {
					// returned object that is not in the pool any more
					LOGGER.error("returned object is not in the pool any more");
				}
			}

		});
	}

}
