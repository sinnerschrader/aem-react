package com.sinnerschrader.aem.react;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;

/**
 *
 * javascript in the browser and in node are executed in a single thread. There
 * are no locking mechanisms in the javascript language. Hence, we are using a
 * pool to get a javascript engine for the current thread.
 *
 * @author stemey
 *
 */
public class JavacriptEnginePoolFactory extends BasePooledObjectFactory<JavascriptEngine> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JavacriptEnginePoolFactory.class);
	private ScriptCollectionLoader loader;
	private Object sling;

	public JavacriptEnginePoolFactory(ScriptCollectionLoader loader, Object sling) {
		this.loader = loader;
		this.sling = sling;
	}

	@Override
	public JavascriptEngine create() throws Exception {
		LOGGER.debug("created new javascript engine");
		JavascriptEngine javascriptEngine = new JavascriptEngine();
		javascriptEngine.initialize(loader, sling);
		return javascriptEngine;
	}

	@Override
	public PooledObject<JavascriptEngine> wrap(JavascriptEngine engine) {
		return new DefaultPooledObject<>(engine);
	}

}
