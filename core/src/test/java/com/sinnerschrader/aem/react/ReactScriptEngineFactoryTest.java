package com.sinnerschrader.aem.react;

import java.io.StringReader;
import java.util.*;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.ObservationManager;

import com.sinnerschrader.aem.react.loader.HashedScript;
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
	private List<HashedScript> scripts;

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
		createProperties();
		factory.initialize(componentContext, null);
	}

	private void createProperties() throws UnsupportedRepositoryOperationException, RepositoryException {
		Dictionary<String, Object> properties = new Hashtable<>();
		String[] scripts = new String[] { "/scripts/test.js" };
		List<HashedScript> hashedList = new LinkedList<>();
		hashedList.add(new HashedScript("1", "function hello() { console.log('hi'); }", "1"));
		properties.put(ReactScriptEngineFactory.PROPERTY_SCRIPTS_PATHS, scripts);
		Mockito.when(componentContext.getProperties()).thenReturn(properties);
		Mockito.when(scriptLoader.loadJcrScript("/scripts/test.js", "")).thenReturn(new StringReader(""));
		Mockito.when(this.scripts.iterator()).thenReturn(hashedList.iterator());
		Mockito.when(repositoryConnectionFactory.getConnection("")).thenReturn(repositoryConnection);
		Mockito.when(repositoryConnection.getObservationManager()).thenReturn(observationManager);
	}

	@Test
	public void testBindDynamicClassLoaderManager() {
		factory.bindDynamicClassLoaderManager(dynamicClassLoaderManager);
	}

	@Test
	public void testUnbindDynamicClassLoaderManager() {
		factory.unbindDynamicClassLoaderManager(dynamicClassLoaderManager);
	}

	@Test
	public void testStop() throws RepositoryException {
		createProperties();
		factory.initialize(componentContext, null);
		factory.stop();
	}

	@Test
	public void testReconfigure() throws RepositoryException {
		createProperties();
		factory.initialize(componentContext, null);
		factory.reconfigure(componentContext, null);
	}
}
