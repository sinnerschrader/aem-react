package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.metrics.MetricsHelper;

@SuppressWarnings("PackageAccessibility")
public class JavascriptEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavascriptEngine.class);

	private final ScriptEngine engine;
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

	public JavascriptEngine() {
		engine = new ScriptEngineManager(null).getEngineByName("nashorn");
		engine.getContext().setErrorWriter(new Print());
		engine.getContext().setWriter(new Print());
	}

	public synchronized void compileScript(List<String> scripts) {
		try {
			StringBuilder jsSource = new StringBuilder();
			for (String script : scripts) {
				jsSource.append(script);
				jsSource.append(";\n");
			}
			script = ((Compilable) engine).compile(jsSource.toString());
		} catch (ScriptException e) {
			throw new TechnicalException("script compilation failure",e);
		}
	}

	public Bindings createBindings() {
		if (script == null) {
			throw new TechnicalException("unable to create bindings. Compiled script is null.");
		}

		try {
			final Bindings bindings = script.getEngine().createBindings();
			bindings.put("console", new Console());
			script.eval(bindings);
			return bindings;
		} catch (ScriptException e) {
			LOGGER.error("unable to create bindings", e);
			throw new TechnicalException("unable to create bindings");
		}
	}

}