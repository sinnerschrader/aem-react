package com.sinnerschrader.aem.react;

import com.sinnerschrader.aem.react.api.Cqx;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.util.List;
import java.util.Map;

public class ReactRenderEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavascriptEngine.class);

    private Bindings bindings;

    ReactRenderEngine(Bindings bindings) {
        this.bindings = bindings;
    }

    public ReactScriptEngine.RenderResult render(String path, String resourceType, int rootNo, String wcmmode, Cqx cqx, boolean renderAsJson,
                                                 List<String> selectors) {

        long startTime = System.currentTimeMillis();

        try {
            long start;

            start = System.currentTimeMillis();
            final ScriptObjectMirror aemGlobal = (ScriptObjectMirror) bindings.get("AemGlobal");
            ScriptObjectMirror mirror = (ScriptObjectMirror) aemGlobal.get("renderReactComponent");
            LOGGER.debug("rre: get renderReactComponent took: " + (System.currentTimeMillis() - start) + "ms");

            start = System.currentTimeMillis();
            Object value = mirror.call(null, path, resourceType, String.valueOf(rootNo), wcmmode,
                    renderAsJson, selectors.toArray(new String[selectors.size()]), cqx);
            LOGGER.debug("rre: call renderReactComponent took: " + (System.currentTimeMillis() - start) + "ms");

            ReactScriptEngine.RenderResult result = new ReactScriptEngine.RenderResult();
            result.html = (String) ((Map<String, Object>) value).get("html");
            result.cache = ((Map<String, Object>) value).get("state").toString();
            LOGGER.debug("rre: render took: " + (System.currentTimeMillis() - startTime) + "ms");
            return result;
        } catch (Exception e) {
            LOGGER.error("error", e);
            throw new TechnicalException("cannot render react on server", e);
        }
    }
}
