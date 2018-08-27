package com.sinnerschrader.aem.react;

import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import com.sinnerschrader.aem.react.metrics.MetricsHelper;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 *
 * This Javascript engine can render ReactJs component in nashorn.
 *
 */
@SuppressWarnings("PackageAccessibility")
public class JavascriptEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavascriptEngine.class);

    private static ScriptEngine engine;

    private ScriptCollectionLoader loader;
    private Map<String, String> scriptChecksums;
    private boolean initialized = false;
    private CompiledScript compiledScript;

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

    public JavascriptEngine(ScriptCollectionLoader loader) {
        this.loader = loader;
    }

    /**
     * initialize this instance. creates a javascript engine and loads the
     * javascript files.
     *
     * This needs only be done once, thus the method os synchronized
     */
    public synchronized void initialize(boolean forceInitialization) throws TechnicalException {
        if(this.initialized && !forceInitialization) {
            return;
        }

        initScriptEngine();

        try {
            long start = System.currentTimeMillis();
            compiledScript = compileScript();
            LOGGER.debug("jse: compileScript took: " + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();
            Bindings bindings = compiledScript.getEngine().createBindings();
            compiledScript.eval(bindings);
            LOGGER.debug("jse: warm up took: " + (System.currentTimeMillis() - start) + "ms");

            this.initialized = true;
        } catch (ScriptException e) {
            LOGGER.error("jse: unable to initialize script", e);
            throw new TechnicalException("unable to initialize jse");
        }
    }

    private void initScriptEngine() {
        if (engine != null) {
            return;
        }

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager(null);
        engine = scriptEngineManager.getEngineByName("nashorn");
        engine.getContext().setErrorWriter(new Print());
        engine.getContext().setWriter(new Print());
        engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("console", new Console());
    }

    private CompiledScript compileScript() throws ScriptException {
        scriptChecksums = new HashMap<>();
        Iterator<HashedScript> iterator = loader.iterator();
        String script = "";
        while (iterator.hasNext()) {
            HashedScript next = iterator.next();
            script += next.getScript() + ";\n";
            scriptChecksums.put(next.getId(), next.getChecksum());
        }

        return ((Compilable) engine).compile(script);
    }

    /**
     * render the given react component
     * @return
     */
    public RenderResult render(String path, String resourceType, int rootNo, String wcmmode, Cqx cqx, boolean renderAsJson,
                               List<String> selectors) {

        long startTime = System.currentTimeMillis();

        if(!this.initialized) {
            throw new IllegalStateException("jse: not initialized");
        }

        try {
            long start;

            start = System.currentTimeMillis();
            Bindings bindings = compiledScript.getEngine().createBindings();
            LOGGER.debug("jse: create bindings: " + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();
            compiledScript.eval(bindings);
            LOGGER.debug("jse: eval bindings " + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();
            final ScriptObjectMirror aemGlobal = (ScriptObjectMirror) bindings.get("AemGlobal");
            ScriptObjectMirror mirror = (ScriptObjectMirror) aemGlobal.get("renderReactComponent");
            LOGGER.debug("jse: get renderReactComponent " + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();
            Object value = mirror.call(null, path, resourceType, String.valueOf(rootNo), wcmmode,
                    renderAsJson, selectors.toArray(new String[selectors.size()]), cqx);
            LOGGER.debug("jse: call renderReactComponent " + (System.currentTimeMillis() - start) + "ms");

            RenderResult result = new RenderResult();
            result.html = (String) ((Map<String, Object>) value).get("html");
            result.cache = ((Map<String, Object>) value).get("state").toString();
            LOGGER.debug("jse: render took: " + (System.currentTimeMillis() - startTime) + "ms");
            return result;
        } catch (Exception e) {
            LOGGER.error("error", e);
            throw new TechnicalException("cannot render react on server", e);
        }
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