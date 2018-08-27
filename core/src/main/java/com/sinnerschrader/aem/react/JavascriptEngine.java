package com.sinnerschrader.aem.react;

import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;

import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.cache.CacheKey;
import com.sinnerschrader.aem.react.cache.ComponentCache;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import com.sinnerschrader.aem.react.metrics.MetricsHelper;
import com.sinnerschrader.aem.react.node.NodeRenderer;

/**
 * This Javascript engine can render ReactJs component in nashorn.
 *
 * @author stemey
 */
@Slf4j
public class JavascriptEngine {

	private ScriptCollectionLoader loader;
	private ScriptEngine engine;
	private Map<String, String> scriptChecksums;
	private boolean initialized = false;
	private Object sling;
	private CqxHolder cqxHolder;
	private ComponentCache cache;

	private final boolean nodeRendererEnabled;
	private final NodeRenderer nodeRenderer;

	public static class Console {

		public Console() {
			super();
		}

		public void debug(String statement, Object... args) {
			LOG.debug(statement, args);
		}

		public void debug(String statement, Object error) {
			LOG.debug(statement, error);
		}

		public void log(String statement) {
			LOG.info(statement);
		}

		public void log(String statement, Object error) {
			LOG.info(statement, error);
		}

		public void info(String statement) {
			LOG.info(statement);
		}

		public void info(String statement, Object error) {
			LOG.info(statement, error);
		}

		public void error(String statement) {
			LOG.error(statement);
		}

		public void error(String statement, Object error) {
			LOG.error(statement, error);
		}

		public void warn(String statement) {
			LOG.warn(statement);
		}

		public void warn(String statement, Object error) {
			LOG.warn(statement, error);
		}

		public void time(String name) {
			MetricsHelper.getCurrent().timer(name);
		}

		public void timeEnd(String name) {
			MetricsHelper.getCurrent().timerEnd(name);
		}

	}

	public static class Print extends Writer {
		@Override
		public void write(char[] cbuf, int off, int len) {
			LOG.error(new String(cbuf, off, len));
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	}

	public JavascriptEngine(
			ScriptCollectionLoader loader, Object sling, ComponentCache cache,
			boolean nodeRendererEnabled, NodeRenderer nodeRenderer
	) {
		this.loader = loader;
		this.sling = sling;
		this.cache = cache;
		this.nodeRendererEnabled = nodeRendererEnabled;
		this.nodeRenderer = nodeRenderer;
	}

	/**
	 * initialize this instance. creates a javascript engine and loads the
	 * javascript files. Instances of this class are not thread-safe.
	 */
	public void initialize() {
		if (this.initialized) {
			return;
		}

		ScriptEngineManager scriptEngineManager = new ScriptEngineManager(null);
		engine = scriptEngineManager.getEngineByName("nashorn");
		engine.getContext().setErrorWriter(new Print());
		engine.getContext().setWriter(new Print());
		engine.put("console", new Console());
		engine.put("Sling", this.sling);
		this.cqxHolder = new CqxHolder();
		engine.put("Cqx", this.cqxHolder);
		loadJavascriptLibrary();

		this.initialized = true;
	}

	private void loadJavascriptLibrary() {
		long start = System.currentTimeMillis();
		scriptChecksums = new HashMap<>();
		Iterator<HashedScript> iterator = loader.iterator();
		while (iterator.hasNext()) {
			try {
				HashedScript next = iterator.next();
				engine.eval(next.getScript());
				scriptChecksums.put(next.getId(), next.getChecksum());

			} catch (ScriptException e) {
				throw new TechnicalException("cannot eval library script", e);
			}
		}
		LOG.debug("JavascriptEngine.loadJavascriptLibrary took: " + (System.currentTimeMillis() - start) + "ms");
	}

	/**
	 * render the given react component
	 *
	 * @param path
	 * @param resourceType
	 * @param wcmmode
	 * @param cqx
	 *            API object for current request
	 * @return
	 */
	public RenderResult render(SlingHttpServletRequest request, String path, String resourceType, int rootNo,
			String wcmmode, Cqx cqx, boolean renderAsJson, List<String> selectors) {

		if (!this.initialized) {
			throw new IllegalStateException("JavascriptEngine is not initialized");
		}

		return cache.cache(new CacheKey(path, resourceType, wcmmode, renderAsJson, selectors), request, path,
				resourceType, (Object cacheableModel) -> {

					if (cacheableModel != null && nodeRendererEnabled && nodeRenderer.supports(resourceType)) {
						return nodeRenderer.render(path, resourceType, wcmmode, selectors.toArray(new String[0]), cacheableModel);
					}

					Invocable invocable = ((Invocable) engine);
					try {
						this.cqxHolder.init(cqx);
						Object AemGlobal = engine.get("AemGlobal");
						Object value = invocable.invokeMethod(AemGlobal, "renderReactComponent", path, resourceType,
								String.valueOf(rootNo), wcmmode, renderAsJson,
								selectors.toArray(new String[0]));

						RenderResult result = new RenderResult();
						result.html = (String) ((Map<String, Object>) value).get("html");
						result.cache = ((Map<String, Object>) value).get("state").toString();

						return result;
					} catch (NoSuchMethodException | ScriptException e) {
						throw new TechnicalException("cannot render react on server", e);
					}
				});
	}

	public ScriptEngine getEngine() {
		return engine;
	}

	public boolean isScriptsChanged() {
		Iterator<HashedScript> iterator = loader.iterator();
		if (!iterator.hasNext() && scriptChecksums.size() > 0) {
			return true;
		}
		while (iterator.hasNext()) {
			HashedScript next = iterator.next();
			String checksum = scriptChecksums.get(next.getId());
			if (checksum == null || !checksum.equals(next.getChecksum())) {
				return true;
			}
		}
		return false;
	}

}
