package com.sinnerschrader.aem.react;

import com.sinnerschrader.aem.react.node.NodeRenderer;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.sinnerschrader.aem.react.cache.ComponentCache;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;

/**
 * javascript in the browser and in node are executed in a single thread. There
 * are no locking mechanisms in the javascript language. Hence, we are using a
 * pool to get a javascript engine for the current thread.
 *
 * @author stemey
 */
public class JavacriptEnginePoolFactory extends BasePooledObjectFactory<JavascriptEngine> {

	private final ScriptCollectionLoader loader;
	private final Object sling;
	private final ComponentCache cache;

	private final boolean nodeRendererEnabled;
	private final NodeRenderer nodeRenderer;

	public JavacriptEnginePoolFactory(
			ScriptCollectionLoader loader, Object sling, ComponentCache cache,
			boolean nodeRendererEnabled, NodeRenderer nodeRenderer
	) {
		this.loader = loader;
		this.sling = sling;
		this.cache = cache;
		this.nodeRendererEnabled = nodeRendererEnabled;
		this.nodeRenderer = nodeRenderer;
	}

	@Override
	public JavascriptEngine create() {
		return new JavascriptEngine(loader, sling, cache, nodeRendererEnabled, nodeRenderer);
	}

	@Override
	public PooledObject<JavascriptEngine> wrap(JavascriptEngine engine) {
		return new DefaultPooledObject<>(engine);
	}

}
