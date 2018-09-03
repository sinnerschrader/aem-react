package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.cache.ComponentCache;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.json.ObjectMapperFactory;
import com.sinnerschrader.aem.react.loader.JcrResourceChangeListener;
import com.sinnerschrader.aem.react.loader.ScriptLoader;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;
import com.sinnerschrader.aem.react.repo.RepositoryConnectionFactory;
import com.sinnerschrader.aem.reactapi.json.CacheView;

@Component(label = "ReactJs Script Engine Factory", metatype = true)
@Service(ScriptEngineFactory.class)
@Properties({ @Property(name = "service.description", value = "Reactjs Templating Engine"), //
		@Property(name = "compatible.javax.script.name", value = "jsx"),
		@Property(name = ReactScriptEngineFactory.PROPERTY_SCRIPTS_PATHS, label = "the jcr paths to the scripts libraries", value = {}, cardinality = Integer.MAX_VALUE), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_SUBSERVICENAME, label = "the subservicename for accessing the script resources. If it is null then the deprecated system admin will be used.", value = ""), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_CACHE_MAX_SIZE, label = "component cache max size", longValue = 2000), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_CACHE_MAX_MINUTES, label = "component cache max minutes", longValue = 10), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_CACHE_DEBUG, label = "debug cache", boolValue = false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ROOT_ELEMENT_NAME, label = "the root element name", value = "div"), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ROOT_CLASS_NAME, label = "the root element class name", value = ""), //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_INCLUDE_PATTERN, label = "pattern for text properties in sling models that must be mapped by resource resolver", value = "^/content"), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ENABLE_REVERSE_MAPPING, label = "incoming sling mapping is not supported", description = "If the incoming sling mapping is not supported, then this optioen will make sure aem react works as expected", boolValue = false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_MAPPING_DISABLE, label = "Disable sling mapping in react completely", description = "check this option to disable sling mapping in aem react.", boolValue = false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_MANGLE_NAMESPACES, label = "mangles namespaces", description = "tell aemr eact how slingmapping handles namespace mangling.", boolValue = true), //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_EXCLUDE_PATTERN, label = "pattern for text properties in sling models that must NOT be mapped by resource resolver", value = ""), //
		@Property(name = ReactScriptEngineFactory.MAX_RENDER_ENGINE_SIZE, label = "max number of react render engines. Use (num_of_cpus/2)+1 as start value.", value = "5") //
})
public class ReactScriptEngineFactory extends AbstractScriptEngineFactory {

    public static final String PROPERTY_SCRIPTS_PATHS = "scripts.paths";
    public static final String PROPERTY_SUBSERVICENAME = "subServiceName";
    public static final String PROPERTY_CACHE_MAX_SIZE = "cache.max.size";
    public static final String PROPERTY_CACHE_DEBUG = "cache.debug";
    public static final String PROPERTY_CACHE_MAX_MINUTES = "cache.max.minutes";
    public static final String PROPERTY_ROOT_ELEMENT_NAME = "root.element.name";
    public static final String PROPERTY_ROOT_CLASS_NAME = "root.element.class.name";
    public static final String PROPERTY_ENABLE_REVERSE_MAPPING = "mapping.reverse.enable";
    public static final String PROPERTY_MAPPING_DISABLE = "mapping.disable";
    public static final String PROPERTY_MANGLE_NAMESPACES = "mapping.mangle.namespaces";
    public static final String JSON_RESOURCEMAPPING_INCLUDE_PATTERN = "json.resourcemapping.include.pattern";
    public static final String JSON_RESOURCEMAPPING_EXCLUDE_PATTERN = "json.resourcemapping.exclude.pattern";
    public static final String MAX_RENDER_ENGINE_SIZE = "max.render.engine.size";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactScriptEngineFactory.class);
    private static final String NASHORN_POLYFILL_JS = "nashorn-polyfill.js";

	@Reference
	private ModelFactory modelFactory;

	@Reference
	private DynamicClassLoaderManager dynamicClassLoaderManager;

	@Reference
	private OsgiServiceFinder finder;

	@Reference
	private ScriptLoader scriptLoader;

	@Reference
	private AdapterManager adapterManager;

	@Reference
	private ComponentMetricsService metricsService;

    @Reference
    private RepositoryConnectionFactory repositoryConnectionFactory;

	private ClassLoader dynamicClassLoader;
	private ReactScriptEngine engine;
	private String[] scriptResources;
	private JcrResourceChangeListener listener;
	private String subServiceName;
	private ComponentCache cache;
	private PoolManager poolManager;

    public ReactScriptEngineFactory() {
        super();
        setNames("reactjs");
        setExtensions("jsx");
    }

	@Override
	public String getLanguageName() {
		return "jsx";
	}

	@Override
	public String getLanguageVersion() {
		return "1.0.0";
	}

    @Modified
    public void reconfigure(final ComponentContext context, Map<String, Object> properties) throws RepositoryException {
        stop();
        initialize(context);
    }

    @Deactivate
    public void stop() throws RepositoryException {
        if (this.engine != null) {
            this.engine.stop();
        }
        this.listener.deactivate();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return engine;
    }

    @Activate
    public void initialize(final ComponentContext context) {
        subServiceName = PropertiesUtil.toString(context.getProperties().get(PROPERTY_SUBSERVICENAME), "");
        scriptResources = PropertiesUtil.toStringArray(context.getProperties().get(PROPERTY_SCRIPTS_PATHS), new String[0]);
        int maxSize = PropertiesUtil.toInteger(context.getProperties().get(PROPERTY_CACHE_MAX_SIZE), 2000);
        int maxMinutes = PropertiesUtil.toInteger(context.getProperties().get(PROPERTY_CACHE_MAX_MINUTES), 10);
        int maxRendererSize = PropertiesUtil.toInteger(context.getProperties().get(MAX_RENDER_ENGINE_SIZE), 5);
        String rootElementName = PropertiesUtil.toString(context.getProperties().get(PROPERTY_ROOT_ELEMENT_NAME),"div");
        String rootElementClassName = PropertiesUtil.toString(context.getProperties().get(PROPERTY_ROOT_CLASS_NAME),"");
        boolean mangleNameSpaces = PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_MANGLE_NAMESPACES), true);
        boolean disableMapping = PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_MAPPING_DISABLE), false);
        boolean debugCache = PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_CACHE_DEBUG), false);
        boolean enableReverseMapping = !disableMapping
                && PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_ENABLE_REVERSE_MAPPING), false);

        String includePattern = disableMapping ? null
                : PropertiesUtil.toString(context.getProperties().get(JSON_RESOURCEMAPPING_INCLUDE_PATTERN),"^/content");
        String excludePattern = disableMapping ? null
                : PropertiesUtil.toString(context.getProperties().get(JSON_RESOURCEMAPPING_EXCLUDE_PATTERN), null);

        ObjectMapper mapper = new ObjectMapperFactory().create(includePattern, excludePattern);
        ObjectWriter cacheWriter = new ObjectMapperFactory().create(includePattern, excludePattern)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).writerWithView(CacheView.class);

        this.cache = new ComponentCache(modelFactory, cacheWriter, maxSize, maxMinutes, metricsService, debugCache);

        try {
            startListener();

            poolManager = new PoolManager(maxRendererSize, metricsService);
            List<String> scripts = createScripts();
            if (scripts.size() > 0) {
                poolManager.updateScripts(scripts);
            }

            this.engine = new ReactScriptEngine(this, poolManager, finder, dynamicClassLoaderManager, rootElementName,
                    rootElementClassName, modelFactory, adapterManager, mapper, metricsService, enableReverseMapping,
                    disableMapping, mangleNameSpaces, cache);
        } catch (Exception e) {
            LOGGER.info("cannot load and listen to script on initialize. will try again later", e);
        }
    }

    protected void bindDynamicClassLoaderManager(final DynamicClassLoaderManager dclm) {
        if (this.dynamicClassLoader != null) {
            this.dynamicClassLoader = null;
            this.dynamicClassLoaderManager = null;
        }
        this.dynamicClassLoaderManager = dclm;
        dynamicClassLoader = dclm.getDynamicClassLoader();
    }

    protected void unbindDynamicClassLoaderManager(final DynamicClassLoaderManager dclm) {
        if (this.dynamicClassLoaderManager == dclm) {
            this.dynamicClassLoader = null;
            this.dynamicClassLoaderManager = null;
        }
    }

    protected ClassLoader getClassLoader() {
        return dynamicClassLoader;
    }

    private List<String> createScripts() {
        List<String> scripts = new LinkedList<>();
        // we need to add the nashorn polyfill for console, global and AemGlobal
        String polyFillName = this.getClass().getPackage().getName().replace(".", "/") + "/" + NASHORN_POLYFILL_JS;

        URL polyFillUrl = this.dynamicClassLoader.getResource(polyFillName);
        if (polyFillUrl == null) {
            throw new TechnicalException("cannot find initial script " + polyFillName);
        }

        try {
            scripts.add(IOUtils.toString(polyFillUrl.openStream(), "UTF-8"));
        } catch (IOException | TechnicalException e) {
            throw new TechnicalException("cannot open stream to " + polyFillUrl, e);
        }

        for (String scriptResource : scriptResources) {
            try (Reader reader = scriptLoader.loadJcrScript(scriptResource, subServiceName)) {
                scripts.add(IOUtils.toString(reader));
            } catch (TechnicalException | IOException e) {
                LOGGER.error("cannot load script resources", e);
            }
        }

        return scripts;
    }

    private void onScriptChanged() {
        if (this.cache != null) {
            this.cache.clear();
        }
        List<String> scripts = createScripts();
        poolManager.updateScripts(scripts);
    }

	private void startListener() {
        if (this.listener != null) {
            return;
        }

        this.listener = new JcrResourceChangeListener(repositoryConnectionFactory, script -> onScriptChanged(),
                subServiceName);
		this.listener.activate(scriptResources);
	}

}
