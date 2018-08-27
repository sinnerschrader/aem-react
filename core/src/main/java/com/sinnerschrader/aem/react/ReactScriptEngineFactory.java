package com.sinnerschrader.aem.react;

import javax.jcr.RepositoryException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sinnerschrader.aem.reactapi.json.CacheView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.service.component.ComponentContext;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.cache.ComponentCache;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.json.ObjectMapperFactory;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.JcrResourceChangeListener;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import com.sinnerschrader.aem.react.loader.ScriptLoader;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;
import com.sinnerschrader.aem.react.node.EditDialogLoader;
import com.sinnerschrader.aem.react.node.NodeRenderer;
import com.sinnerschrader.aem.react.repo.RepositoryConnectionFactory;

@Component(label = "ReactJs Script Engine Factory", metatype = true)
@Service(ScriptEngineFactory.class)
@Properties({
		@Property(name = "service.description", value = "Reactjs Templating Engine"), //
		@Property(name = "compatible.javax.script.name", value = "jsx"),
		@Property(name = ReactScriptEngineFactory.PROPERTY_SCRIPTS_PATHS, label = "the jcr paths to the scripts libraries", value = {}, cardinality = Integer.MAX_VALUE), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_SUBSERVICENAME, label = "the subservicename for accessing the script resources. If it is null then the deprecated system admin will be used.", value = ""), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_POOL_TOTAL_SIZE, label = "total javascript engine pool size", longValue = 20), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_POOL_MIN_SIZE, label = "initial javascript engine pool size", longValue = 0), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_CACHE_MAX_SIZE, label = "component cache max size", longValue = 2000), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_CACHE_MAX_MINUTES, label = "component cache max minutes", longValue = 10), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_CACHE_DEBUG, label = "debug cache", boolValue=false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ROOT_ELEMENT_NAME, label = "the root element name", value = "div"), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ROOT_CLASS_NAME, label = "the root element class name", value = ""), //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_INCLUDE_PATTERN, label = "pattern for text properties in sling models that must be mapped by resource resolver", value = "^/content"), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ENABLE_REVERSE_MAPPING, label = "incoming sling mapping is not supported", description = "If the incoming sling mapping is not supported, then this optioen will make sure aem react works as expected", boolValue = false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_MAPPING_DISABLE, label = "Disable sling mapping in react completely", description = "check this option to disable sling mapping in aem react.", boolValue = false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_MANGLE_NAMESPACES, label = "mangles namespaces", description = "tell aemr eact how slingmapping handles namespace mangling.", boolValue = true), //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_EXCLUDE_PATTERN, label = "pattern for text properties in sling models that must NOT be mapped by resource resolver", value = ""), //

		@Property(
				name = ReactScriptEngineFactory.NODE_RENDERER_ENABLED_KEY,
				label = "Wheter the node renderer should be used",
				description = "Boolean wheter to use the node renderer",
				value = ReactScriptEngineFactory.NODE_RENDERER_ENABLED_DEFAULT_VALUE
		),
		@Property(
				name = ReactScriptEngineFactory.NODE_RENDERER_URL_KEY,
				label = "The url of node renderer",
				description = "The endpoint of the lambda function or node server",
				value = ReactScriptEngineFactory.NODE_RENDERER_URL_DEFAULT
		),
		@Property(
				name = ReactScriptEngineFactory.NODE_RENDERER_RESOURCE_TYPES_KEY,
				label = "The supported resource types of the node renderer",
				description = "A list of resource types that the node / lambda function ca render",
				value = ReactScriptEngineFactory.NODE_RENDERER_RESOURCE_TYPES_M031,
				cardinality = Integer.MAX_VALUE
		),
})
@Slf4j
public class ReactScriptEngineFactory extends AbstractScriptEngineFactory {

	public static final String PROPERTY_SCRIPTS_PATHS = "scripts.paths";
	public static final String PROPERTY_SUBSERVICENAME = "subServiceName";
	public static final String PROPERTY_POOL_TOTAL_SIZE = "pool.total.size";
	public static final String PROPERTY_POOL_MIN_SIZE = "pool.min.size";
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

	static final String NODE_RENDERER_ENABLED_KEY = "noderenderer.enabled";
	static final String NODE_RENDERER_ENABLED_DEFAULT_VALUE = "true";

	static final String NODE_RENDERER_URL_KEY = "noderenderer.url";
	static final String NODE_RENDERER_URL_DEFAULT = "https://euhvipra3j.execute-api.eu-central-1.amazonaws.com/default/renderReactComponent";

	static final String NODE_RENDERER_RESOURCE_TYPES_KEY = "noderenderer.resourcetypes";
	static final String NODE_RENDERER_RESOURCE_TYPES_M031 = "vw-ngw/editorial/components/content/m031_textLink";
	private static final String[] NODE_RENDERER_RESOURCE_TYPES_DEFAULT = {
			NODE_RENDERER_RESOURCE_TYPES_M031
	};

	@Reference
	private ServletResolver servletResolver;

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

	private static final String NASHORN_POLYFILL_JS = "nashorn-polyfill.js";

	private ClassLoader dynamicClassLoader;

	private ReactScriptEngine engine;

	private List<HashedScript> scripts;
	private String[] scriptResources;
	private JcrResourceChangeListener listener;
	private String subServiceName;

	@Reference
	private RepositoryConnectionFactory repositoryConnectionFactory;

	private boolean initialized = false;

	public synchronized void createScripts() {
		List<HashedScript> newScripts = new LinkedList<>();
		// we need to add the nashorn polyfill for console, global and AemGlobal
		String polyFillName = this.getClass().getPackage().getName().replace(".", "/") + "/" + NASHORN_POLYFILL_JS;

		URL polyFillUrl = this.dynamicClassLoader.getResource(polyFillName);
		if (polyFillUrl == null) {
			throw new TechnicalException("cannot find initial script " + polyFillName);
		}
		try {
			newScripts.add(createHashedScript("polyFillUrl", new InputStreamReader(polyFillUrl.openStream(), StandardCharsets.UTF_8)));
		} catch (IOException | TechnicalException e) {
			throw new TechnicalException("cannot open stream to " + polyFillUrl, e);
		}

		for (String scriptResource : scriptResources) {
			try (Reader reader = scriptLoader.loadJcrScript(scriptResource, subServiceName)) {
				newScripts.add(createHashedScript(scriptResource, reader));
			} catch (TechnicalException | IOException e) {
				LOG.error("cannot load script resources", e);
			}
		}
		this.scripts = newScripts;
	}

	private HashedScript createHashedScript(String id, Reader reader) {
		try {
			String script = IOUtils.toString(reader);
			byte[] checksum = MessageDigest.getInstance("MD5").digest(script.getBytes(StandardCharsets.UTF_8));
			return new HashedScript(new String(Base64.getEncoder().encode(checksum), StandardCharsets.UTF_8), script, id);
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new TechnicalException("cannot create hashed script " + id, e);
		}
	}

	protected ScriptCollectionLoader createLoader(final String[] scriptResources) {

		return new ScriptCollectionLoader() {

			@Override
			public Iterator<HashedScript> iterator() {
				return scripts.iterator();
			}
		};

	}

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

	@Activate
	public void initialize(final ComponentContext context, Map<String, Object> properties) {

		ContextPropertyHelper ctxPropHelper = new ContextPropertyHelper(context.getProperties());

		this.subServiceName = ctxPropHelper.toString(PROPERTY_SUBSERVICENAME, "");
		this.scriptResources = ctxPropHelper.toStringArray(PROPERTY_SCRIPTS_PATHS, new String[0]);
		int poolTotalSize = ctxPropHelper.toInteger(PROPERTY_POOL_TOTAL_SIZE, 20);
		int maxSize = ctxPropHelper.toInteger(PROPERTY_CACHE_MAX_SIZE, 0);
		int maxMinutes = ctxPropHelper.toInteger(PROPERTY_CACHE_MAX_MINUTES, 10);
		String rootElementName = ctxPropHelper.toString(PROPERTY_ROOT_ELEMENT_NAME, "div");
		String rootElementClassName = ctxPropHelper.toString(PROPERTY_ROOT_CLASS_NAME,"");
		boolean mangleNameSpaces = ctxPropHelper.toBoolean(PROPERTY_MANGLE_NAMESPACES, true);
		boolean disableMapping = ctxPropHelper.toBoolean(PROPERTY_MAPPING_DISABLE, false);
		boolean debugCache = ctxPropHelper.toBoolean(PROPERTY_CACHE_DEBUG, false);
		boolean enableReverseMapping = !disableMapping
				&& ctxPropHelper.toBoolean(PROPERTY_ENABLE_REVERSE_MAPPING, false);
		ScriptCollectionLoader loader = createLoader(scriptResources);

		String includePattern = disableMapping
				? null
				: ctxPropHelper.toString(JSON_RESOURCEMAPPING_INCLUDE_PATTERN, "^/content");
		String excludePattern = disableMapping
				? null
				: ctxPropHelper.toString(JSON_RESOURCEMAPPING_EXCLUDE_PATTERN, null);

		ObjectMapper mapper = new ObjectMapperFactory().create(includePattern, excludePattern);
		ObjectWriter cacheWriter = new ObjectMapperFactory()
				.create(includePattern, excludePattern)
				.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
				.writerWithView(CacheView.class);

		boolean nodeRendererEnabled = ctxPropHelper.toBoolean(NODE_RENDERER_ENABLED_KEY,
				Boolean.parseBoolean(NODE_RENDERER_ENABLED_DEFAULT_VALUE));
		String nodeRendererUrl = ctxPropHelper.toString(NODE_RENDERER_URL_KEY, NODE_RENDERER_URL_DEFAULT);
		String[] nodeRendererResourceTypes = ctxPropHelper.toStringArray(NODE_RENDERER_RESOURCE_TYPES_KEY,
				NODE_RENDERER_RESOURCE_TYPES_DEFAULT);
		EditDialogLoader editDialogLoader = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		NodeRenderer nodeRenderer = new NodeRenderer(nodeRendererUrl, nodeRendererResourceTypes,
				httpClient, mapper, editDialogLoader);

		ComponentCache cache = new ComponentCache(modelFactory, cacheWriter, maxSize, maxMinutes, metricsService, debugCache);
		JavacriptEnginePoolFactory javacriptEnginePoolFactory = new JavacriptEnginePoolFactory(loader, null,
				cache, nodeRendererEnabled, nodeRenderer);
		ObjectPool<JavascriptEngine> pool = createPool(poolTotalSize, javacriptEnginePoolFactory);

		this.engine = ReactScriptEngine.builder()
				.scriptEngineFactory(this)
				.enginePool(pool)
				.osgiServiceFinder(finder)
				.dynamicClassLoaderManager(dynamicClassLoaderManager)
				.rootElementName(rootElementName)
				.rootElementClass(rootElementClassName)
				.modelFactory(modelFactory)
				.adapterManager(adapterManager)
				.objectMapper(mapper)
				.metricsService(metricsService)
				.enableReverseMapping(enableReverseMapping)
				.disableMapping(disableMapping)
				.mangleNameSpaces(mangleNameSpaces)
				.build();
		try {
			initialized = false;
			initializeScripts();
			int minEngineCount = ctxPropHelper.toInteger(PROPERTY_POOL_MIN_SIZE, 5);
			initializeEngines(pool, minEngineCount);
		} catch (Exception e) {
			LOG.info("cannot load and listen to script on initialize. will try again later", e);
		}
	}

	private synchronized void initializeScripts() {
		if (this.initialized) {
			return;
		}
		this.createScripts();
		startListener();
		this.initialized = true;
	}

	private void startListener() {
		this.listener = new JcrResourceChangeListener(
				repositoryConnectionFactory,
				script -> createScripts(),
				subServiceName
		);
		this.listener.activate(scriptResources);
	}

	@Modified
	public void reconfigure(final ComponentContext context, Map<String, Object> properties) throws RepositoryException {
		stop();
		initialize(context, properties);
	}

	@Deactivate
	public void stop() throws RepositoryException {
		initialized = false;
		this.engine.stop();
		this.listener.deactivate();
	}

	protected ObjectPool<JavascriptEngine> createPool(
			int poolTotalSize,
			JavacriptEnginePoolFactory javacriptEnginePoolFactory
	) {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(poolTotalSize);
		config.setMaxIdle(poolTotalSize);
		config.setMinIdle(0);
		config.setLifo(false);
		config.setFairness(true);
		config.setBlockWhenExhausted(true);
		config.setMaxWaitMillis(10000);
		config.setJmxEnabled(true);
		return new GenericObjectPool<>(javacriptEnginePoolFactory, config);
	}

	@Override
	public ScriptEngine getScriptEngine() {
		if (!initialized) {
			initializeScripts();
		}
		return engine;
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

	private void initializeEngines(ObjectPool<JavascriptEngine> pool, int minEngineCount) {
		try {
			JavascriptEngine[] engines = new JavascriptEngine[minEngineCount];
			for (int i = minEngineCount; i > 0; --i) {
				JavascriptEngine engine = pool.borrowObject();
				engine.initialize();
				engines[i - 1] = engine;
			}

			for (int i = minEngineCount; i > 0; --i) {
				pool.returnObject(engines[i - 1]);
			}

			LOG.info(pool.getNumActive() + pool.getNumIdle() + " engines initialized");
		} catch (Exception e) {
			LOG.error("Unable to initialize script engines", e);
		}
	}
}
