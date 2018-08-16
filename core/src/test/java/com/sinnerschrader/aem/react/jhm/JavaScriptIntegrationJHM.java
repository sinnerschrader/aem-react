package com.sinnerschrader.aem.react.jhm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinnerschrader.aem.react.JavascriptEngine;
import com.sinnerschrader.aem.react.ReactScriptEngine;
import com.sinnerschrader.aem.react.ReactScriptEngineFactory;
import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.api.Sling;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.servlethelpers.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.openjdk.jmh.annotations.*;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// migrated com.sinnerschrader.aem.react.IntegrationTest for JHM
public class JavaScriptIntegrationJHM {

    public static class JhmReactScriptEngine extends ReactScriptEngine {

        JhmReactScriptEngine(ReactScriptEngineFactory scriptEngineFactory, ObjectPool<JavascriptEngine> enginePool, OsgiServiceFinder finder, DynamicClassLoaderManager dynamicClassLoaderManager, String rootElementName, String rootElementClass, ModelFactory modelFactory, AdapterManager adapterManager, ObjectMapper mapper, ComponentMetricsService metricsService, boolean enableReverseMapping, boolean disableMapping, boolean mangleNameSpaces) {
            super(scriptEngineFactory, enginePool, finder, dynamicClassLoaderManager, rootElementName, rootElementClass, modelFactory, adapterManager, mapper, metricsService, enableReverseMapping, disableMapping, mangleNameSpaces);
        }
    }

    public static class JhmReactScriptEngineFactory extends ReactScriptEngineFactory {

        @Override
        protected ClassLoader getClassLoader() {
            return super.getClassLoader();
        }
    }

    public static class JhmSlingContextImpl extends SlingContextImpl {

        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        public void tearDown() {
            super.tearDown();
        }
    }

    @State(Scope.Benchmark)
    public static class JavaScriptIntegrationState {

        static String resourceType = "react-demo/components/text";
        static String path = "/content/page/test";
        static String content = "Hallo";

        JhmReactScriptEngineFactory factory;
        DynamicClassLoaderManager dynamicClassLoaderManager;
        ScriptCollectionLoader loader;
        MockRequestDispatcherFactory requestDispatcherFactory;
        ObjectPool<JavascriptEngine> enginePool;
        SlingScriptHelper sling;
        JhmSlingContextImpl slingContext;
        RequestDispatcher dispatcher;
        ModelFactory modelFactory;
        ObjectMapper objectMapper;

        @SuppressWarnings("unchecked")
        @Setup
        public void setUp() {
            factory = mock(JhmReactScriptEngineFactory.class);
            loader = mock(ScriptCollectionLoader.class);
            requestDispatcherFactory = mock(MockRequestDispatcherFactory.class);
            dynamicClassLoaderManager = mock(DynamicClassLoaderManager.class);
            dispatcher = mock(RequestDispatcher.class);
            enginePool = mock(ObjectPool.class);
            modelFactory = mock(ModelFactory.class);
            sling = mock(SlingScriptHelper.class);

            objectMapper = new ObjectMapper();

            slingContext = new JhmSlingContextImpl();
            slingContext.setUp();

            Resource resource = slingContext.create().resource(path,
                    "sling:resourceType", resourceType,
                    "content", content);
            slingContext.currentResource(resource);
        }

        @TearDown
        public void tearDown() {
            slingContext.tearDown();
        }
    }

    @Benchmark
    public void testRenderText(JavaScriptIntegrationState state) throws Exception {
        String html = render(state);
        Document doc = Jsoup.parse(html);

        Element wrapper = getWrapper(doc);

        assertEquals("test xxx", wrapper.attr("class"));
        assertEquals("span", wrapper.nodeName());

        Element textarea = getTextarea(doc);
        ObjectNode jsonFromTextArea = getJsonFromTextArea(textarea);
        assertEquals("<span data-reactroot=\"\">Hallo</span>", wrapper.html());
        assertEquals(JavaScriptIntegrationState.resourceType, jsonFromTextArea.get("resourceType").asText());
        assertEquals(JavaScriptIntegrationState.path, jsonFromTextArea.get("path").asText());
        assertEquals(JavaScriptIntegrationState.content, jsonFromTextArea.get("cache").get("transforms").get(JavaScriptIntegrationState.path).get("content").asText());
    }

    private String render(JavaScriptIntegrationState state) throws Exception {
        when(state.loader.iterator()).thenAnswer((InvocationOnMock mock) -> createScripts().iterator());

        when(state.requestDispatcherFactory.getRequestDispatcher(any(Resource.class), any(RequestDispatcherOptions.class)))
                .thenReturn(state.dispatcher);
        doAnswer((InvocationOnMock invoke) -> {
            ((SlingHttpServletResponse)invoke.getArguments()[1]).getWriter().println("<div><cq data=\"some\"/></div>");
            return null;
        }).when(state.dispatcher).include(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
        state.slingContext.request().setRequestDispatcherFactory(state.requestDispatcherFactory);
        JavascriptEngine jsEngine = new JavascriptEngine();
        ScriptContext scriptContext = new SimpleScriptContext();
        jsEngine.initialize(state.loader, new Sling(scriptContext));

        ReactScriptEngine r = new JhmReactScriptEngine(state.factory, state.enginePool, null, state.dynamicClassLoaderManager, "span",
                "test xxx", state.modelFactory, null, state.objectMapper, new ComponentMetricsService(), false, false, false);
        ClassLoader classLoader = this.getClass().getClassLoader();
        when(state.factory.getClassLoader()).thenReturn(classLoader);
        when(state.dynamicClassLoaderManager.getDynamicClassLoader()).thenReturn(classLoader);

        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(SlingBindings.REQUEST, state.slingContext.request());
        bindings.put(SlingBindings.RESPONSE, state.slingContext.response());
        bindings.put(SlingBindings.SLING, state.sling);

        when(state.enginePool.borrowObject()).thenReturn(jsEngine);

        StringWriter writer = new StringWriter();
        scriptContext.setWriter(writer);
        ReactScriptEngine.RenderResult renderResult = (ReactScriptEngine.RenderResult) r.eval(new StringReader(""), scriptContext);
        assertNull(renderResult);

        return writer.getBuffer().toString();
    }

    private List<HashedScript> createScripts() throws IOException {
        String script1 = IOUtils.toString(getClass().getResource("/com/sinnerschrader/aem/react/nashorn-polyfill.js"));
        String script2 = IOUtils.toString(getClass().getResource("/ts/reactserver.js"));

        List<HashedScript> scripts = new ArrayList<>();
        scripts.add(new HashedScript("ff1", script1, "/script1"));
        scripts.add(new HashedScript("ff2", script2, "/script2"));
        return scripts;
    }

    private Element getWrapper(Document doc) {
        Elements es = doc.select("[data-react=\"app\"]");
        return es.get(0);
    }

    private Element getTextarea(Document doc) {
        Elements es = doc.select("textarea");
        return es.get(0);
    }

    private ObjectNode getJsonFromTextArea(Element ta) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode json = (ObjectNode) objectMapper.readTree(ta.html());
        return json;
    }
}
