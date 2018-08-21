package com.sinnerschrader.aem.react;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;
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
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.json.ObjectMapperFactory;
import com.sinnerschrader.aem.react.loader.HashedScript;
import com.sinnerschrader.aem.react.loader.JcrResourceChangeListener;
import com.sinnerschrader.aem.react.loader.ScriptCollectionLoader;
import com.sinnerschrader.aem.react.loader.ScriptLoader;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;
import com.sinnerschrader.aem.react.repo.RepositoryConnectionFactory;

@Component(label = "ReactJs Script Engine Factory", metatype = true)
@Service(ScriptEngineFactory.class)
@Properties({ @Property(name = "service.description", value = "Reactjs Templating Engine"), //
		@Property(name = "compatible.javax.script.name", value = "jsx"),
		@Property(name = ReactScriptEngineFactory.PROPERTY_SCRIPTS_PATHS, label = "the jcr paths to the scripts libraries", value = {}, cardinality = Integer.MAX_VALUE), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_SUBSERVICENAME, label = "the subservicename for accessing the script resources. If it is null then the deprecated system admin will be used.", value = ""), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ROOT_ELEMENT_NAME, label = "the root element name", value = "div"), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ROOT_CLASS_NAME, label = "the root element class name", value = ""), //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_INCLUDE_PATTERN, label = "pattern for text properties in sling models that must be mapped by resource resolver", value = "^/content"), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_ENABLE_REVERSE_MAPPING, label = "incoming sling mapping is not supported", description = "If the incoming sling mapping is not supported, then this optioen will make sure aem react works as expected", boolValue = false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_MAPPING_DISABLE, label = "Disable sling mapping in react completely", description = "check this option to disable sling mapping in aem react.", boolValue = false), //
		@Property(name = ReactScriptEngineFactory.PROPERTY_MANGLE_NAMESPACES, label = "mangles namespaces", description = "tell aemr eact how slingmapping handles namespace mangling.", boolValue = true), //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_EXCLUDE_PATTERN, label = "pattern for text properties in sling models that must NOT be mapped by resource resolver", value = "") //
})
public class ReactScriptEngineFactory extends AbstractScriptEngineFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ReactScriptEngineFactory.class);

	public static final String PROPERTY_SCRIPTS_PATHS = "scripts.paths";
	public static final String PROPERTY_SUBSERVICENAME = "subServiceName";
	public static final String PROPERTY_ROOT_ELEMENT_NAME = "root.element.name";
	public static final String PROPERTY_ROOT_CLASS_NAME = "root.element.class.name";
	public static final String PROPERTY_ENABLE_REVERSE_MAPPING = "mapping.reverse.enable";
	public static final String PROPERTY_MAPPING_DISABLE = "mapping.disable";
	public static final String PROPERTY_MANGLE_NAMESPACES = "mapping.mangle.namespaces";
	public static final String JSON_RESOURCEMAPPING_INCLUDE_PATTERN = "json.resourcemapping.include.pattern";
	public static final String JSON_RESOURCEMAPPING_EXCLUDE_PATTERN = "json.resourcemapping.exclude.pattern";

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

	private static Logger LOGGER = LoggerFactory.getLogger(ReactScriptEngineFactory.class);

	@Reference
	private RepositoryConnectionFactory repositoryConnectionFactory;
	private boolean disableMapping;
	private boolean enableReverseMapping;

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
			newScripts.add(createHashedScript("polyFillUrl", new InputStreamReader(polyFillUrl.openStream(), "UTF-8")));
		} catch (IOException | TechnicalException e) {
			throw new TechnicalException("cannot open stream to " + polyFillUrl, e);
		}

		for (String scriptResource : scriptResources) {
			try (Reader reader = scriptLoader.loadJcrScript(scriptResource, subServiceName)) {
				newScripts.add(createHashedScript(scriptResource, reader));
			} catch (TechnicalException | IOException e) {
				LOGGER.error("cannot load script resources", e);
			}
		}
		this.scripts = newScripts;
	}

	private HashedScript createHashedScript(String id, Reader reader) {
		String script;
		try {
			script = IOUtils.toString(reader);
			byte[] checksum = MessageDigest.getInstance("MD5").digest(script.getBytes("UTF-8"));
			return new HashedScript(new String(Base64.getEncoder().encode(checksum), "UTF-8"), script, id);
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

		this.subServiceName = PropertiesUtil.toString(context.getProperties().get(PROPERTY_SUBSERVICENAME), "");
		scriptResources = PropertiesUtil.toStringArray(context.getProperties().get(PROPERTY_SCRIPTS_PATHS),
				new String[0]);
		String rootElementName = PropertiesUtil.toString(context.getProperties().get(PROPERTY_ROOT_ELEMENT_NAME),
				"div");
		String rootElementClassName = PropertiesUtil.toString(context.getProperties().get(PROPERTY_ROOT_CLASS_NAME),
				"");
		boolean mangleNameSpaces = PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_MANGLE_NAMESPACES),
				true);
		this.disableMapping = PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_MAPPING_DISABLE), false);
		this.enableReverseMapping = !disableMapping
				&& PropertiesUtil.toBoolean(context.getProperties().get(PROPERTY_ENABLE_REVERSE_MAPPING), false);

		String includePattern = disableMapping ? null
				: PropertiesUtil.toString(context.getProperties().get(JSON_RESOURCEMAPPING_INCLUDE_PATTERN),
						"^/content");
		String excludePattern = disableMapping ? null
				: PropertiesUtil.toString(context.getProperties().get(JSON_RESOURCEMAPPING_EXCLUDE_PATTERN), null);

		ObjectMapper mapper = new ObjectMapperFactory().create(includePattern, excludePattern);

		try {
			initialized=false;

			initializeScripts();

			ScriptCollectionLoader loader = createLoader(scriptResources);
			JavascriptEngine jsEngine = new JavascriptEngine(loader, null);
			jsEngine.initialize(true);

			this.engine = new ReactScriptEngine(this, jsEngine, finder, dynamicClassLoaderManager, rootElementName,
					rootElementClassName, modelFactory, adapterManager, mapper, metricsService, enableReverseMapping, disableMapping,
					mangleNameSpaces);
		} catch (Exception e) {
			LOGGER.info("cannot load and listen to script on initialize. will try again later", e);
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
		this.listener = new JcrResourceChangeListener(repositoryConnectionFactory,
				new JcrResourceChangeListener.Listener() {
					@Override
					public void changed(String script) {
						createScripts();
					}

				}, subServiceName);
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
		this.engine = null;
		this.listener.deactivate();
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
}
