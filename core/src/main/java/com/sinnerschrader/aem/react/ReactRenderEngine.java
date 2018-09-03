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
    private String stateHash;

    ReactRenderEngine(Bindings bindings, String stateHash) {
        this.bindings = bindings;
        this.stateHash = stateHash;
    }

    public boolean isValid(String hash) {
        if (stateHash == null) {
            return false;
        }
        return stateHash.equals(hash);
    }

    public ReactScriptEngine.RenderResult render(String path, String resourceType, int rootNo, String wcmmode, Cqx cqx, boolean renderAsJson,
                                                 List<String> selectors) {
        try {
            long startTime = System.currentTimeMillis();
            final ScriptObjectMirror aemGlobal = (ScriptObjectMirror) bindings.get("AemGlobal");
            ScriptObjectMirror mirror = (ScriptObjectMirror) aemGlobal.get("renderReactComponent");

            Object value = mirror.call(null, path, resourceType, String.valueOf(rootNo), wcmmode,
                    renderAsJson, selectors.toArray(new String[selectors.size()]), cqx);

            ReactScriptEngine.RenderResult result = new ReactScriptEngine.RenderResult();
            result.html = (String) ((Map<String, Object>) value).get("html");
            result.cache = ((Map<String, Object>) value).get("state").toString();

            LOGGER.debug("rre: render took: {}ms", (System.currentTimeMillis() - startTime));
            return result;
        } catch (Exception e) {
            throw new TechnicalException("cannot render react on server", e);
        }
    }
}
