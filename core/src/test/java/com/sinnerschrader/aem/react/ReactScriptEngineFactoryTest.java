package com.sinnerschrader.aem.react;

import java.io.StringReader;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;

import com.sinnerschrader.aem.react.api.OsgiServiceFinder;
import com.sinnerschrader.aem.react.loader.ScriptLoader;
import com.sinnerschrader.aem.react.metrics.ComponentMetricsService;
import com.sinnerschrader.aem.react.repo.RepositoryConnection;
import com.sinnerschrader.aem.react.repo.RepositoryConnectionFactory;

import junitx.util.PrivateAccessor;

@RunWith(MockitoJUnitRunner.class)
public class ReactScriptEngineFactoryTest {

	@Rule
	public SlingContext slingContext = new SlingContext();

	@Mock
	private ServletResolver servletResolver;

	@Mock
	private ModelFactory modelFactory;

	@Mock
	private DynamicClassLoaderManager dynamicClassLoaderManager;

	@Mock
	private OsgiServiceFinder finder;

	@Mock
	private RepositoryConnectionFactory repositoryConnectionFactory;
	@Mock
	private RepositoryConnection repositoryConnection;

	@Mock
	private ObservationManager observationManager;

	@Mock
	private ScriptLoader scriptLoader;

	@Mock
	private AdapterManager adapterManager;

	@Mock
	private ComponentMetricsService metricsService;

	@Mock
	private ComponentContext componentContext;

	@InjectMocks
	private ReactScriptEngineFactory factory = new ReactScriptEngineFactory();

	@Before
	public void setup() throws NoSuchFieldException {
		PrivateAccessor.setField(factory, "dynamicClassLoader", ReactScriptEngineFactory.class.getClassLoader());
	}

	@Test
	public void test() throws UnsupportedRepositoryOperationException, RepositoryException {

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(ReactScriptEngineFactory.PROPERTY_SCRIPTS_PATHS, new String[] { "/scripts/test.js" });
		Mockito.when(componentContext.getProperties()).thenReturn(properties);
		Mockito.when(scriptLoader.loadJcrScript("/scripts/test.js", "")).thenReturn(new StringReader(""));
		Mockito.when(repositoryConnectionFactory.getConnection("")).thenReturn(repositoryConnection);
		Mockito.when(repositoryConnection.getObservationManager()).thenReturn(observationManager);
		factory.initialize(componentContext, null);
	}

}
