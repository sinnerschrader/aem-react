package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import com.sinnerschrader.aem.react.metrics.MetricsHelper;

@SuppressWarnings("PackageAccessibility")
public class JavascriptEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavascriptEngine.class);

	private final ScriptCollectionLoader loader;
	private final ScriptEngine engine;
	private final boolean reloadScriptOnChange;

	private final Map<String, String> scriptChecksums = new ConcurrentHashMap<>();
	private CompiledScript script;

	public static class Console {

		public Console() {
			super();
		}

		public void debug(String statement, Object... args) {
			LOGGER.debug(statement, args);
		}

		public void debug(String statement, Object error) {
			LOGGER.debug(statement, error);
		}

		public void log(String statement) {
			LOGGER.info(statement);
		}

		public void log(String statement, Object error) {
			LOGGER.info(statement, error);
		}

		public void info(String statement) {
			LOGGER.info(statement);
		}

		public void info(String statement, Object error) {
			LOGGER.info(statement, error);
		}

		public void error(String statement) {
			LOGGER.error(statement);
		}

		public void error(String statement, Object error) {
			LOGGER.error(statement, error);
		}

		public void warn(String statement) {
			LOGGER.warn(statement);
		}

		public void warn(String statement, Object error) {
			LOGGER.warn(statement, error);
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
		public void write(char[] cbuf, int off, int len) throws IOException {
			LOGGER.error(new String(cbuf, off, len));
		}

		@Override
		public void flush() throws IOException {

		}

		@Override
		public void close() throws IOException {

		}
	}

	public JavascriptEngine(ScriptCollectionLoader loader, boolean reloadScriptOnChange) {
		this.loader = loader;
		engine = new ScriptEngineManager(null).getEngineByName("nashorn");
		engine.getContext().setErrorWriter(new Print());
		engine.getContext().setWriter(new Print());
		this.reloadScriptOnChange = reloadScriptOnChange;
	}

	public void compileScript() {
		try {
			long start = System.currentTimeMillis();
			StringBuilder jsSource = new StringBuilder();
			// we want only one compile to run
			synchronized (scriptChecksums) {
				boolean needsCompile = false;
				final Map<String, String> tmp = new HashMap<>();
				for (Iterator<HashedScript> iterator = loader.iterator(); iterator.hasNext();) {
					HashedScript next = iterator.next();
					jsSource.append(next.getScript());
					jsSource.append(";\n");
					String foundCheckSum = scriptChecksums.get(next.getId());
					needsCompile = needsCompile || foundCheckSum == null || !foundCheckSum.equals(next.getChecksum());
					tmp.put(next.getId(), next.getChecksum());
				}
				final Set<Map.Entry<String, String>> toDel = scriptChecksums.entrySet();
				scriptChecksums.putAll(tmp);

				toDel.iterator().forEachRemaining((Map.Entry<String, String> item) -> {
					if (tmp.get(item.getKey()) == null) {
						scriptChecksums.remove(item.getKey());
						LOGGER.debug("jse: removed script: {}", item.getKey());
					}
				});
				if (needsCompile) {
					script = ((Compilable) engine).compile(jsSource.toString());
				}
			}
			LOGGER.debug("jse: compileScript took: {}ms", (System.currentTimeMillis() - start));
		} catch (ScriptException e) {
			throw new TechnicalException("script compilation failure",e);
		}
	}

	public Bindings createBindings() {
		try {
			final long start = System.currentTimeMillis();
			final Bindings bindings = script.getEngine().createBindings();
			bindings.put("console", new Console());
			script.eval(bindings);
			LOGGER.debug("jse: create bindings took: {}ms", (System.currentTimeMillis() - start));
			return bindings;
		} catch (ScriptException e) {
			LOGGER.error("unable to create bindings", e);
			throw new TechnicalException("unable to create bindings");
		}
	}

	public boolean isScriptsChanged() {

		Iterator<HashedScript> iterator = loader.iterator();
		if (!iterator.hasNext() && scriptChecksums.size() > 0) {
			return true;
		}
		while (iterator.hasNext()) {
			HashedScript my = iterator.next();
			String checksum = scriptChecksums.get(my.getId());
			if (checksum == null || !checksum.equals(my.getChecksum())) {
				return true;
			}
		}
		return false;
	}

	public void tryCompileScript() {
		if (script==null) {
			this.compileScript();
		}
	}
}