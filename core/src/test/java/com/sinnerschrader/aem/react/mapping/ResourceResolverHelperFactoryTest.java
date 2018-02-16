package com.sinnerschrader.aem.react.mapping;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceResolverHelperFactoryTest {

	@Mock
	private SlingHttpServletRequest request;

	@Rule
	public SlingContext context = new SlingContext();

	@Mock
	private ResourceResolver resolver;

	@Mock
	private Resource resource;

	@Before
	public void before() {

		Mockito.when(request.getResourceResolver()).thenReturn(resolver);
		Mockito.when(request.getResource()).thenReturn(resource);
	}

	@Test
	public void testSimple() {
		expectRequestURI("/de/index.html");
		expectMappingPrefix("", "/content");
		expectResourcePath("/content/de/index/jcr:content/teaser");

		ResourceResolverHelper helper = ResourceResolverHelperFactory.createHelper(request, true);

		Assert.assertEquals("/content/en/william.html", helper.resolveInternally("/en/william.html"));
	}

	@Test
	public void testSelectorsAndSuffix() {

		expectRequestURI("/de/index.s1.html/ahdgasdasd");
		expectMappingPrefix("", "/content");
		expectResourcePath("/content/de/index/jcr:content/teaser");

		ResourceResolverHelper helper = ResourceResolverHelperFactory.createHelper(request, true);

		Assert.assertEquals("/content/en/william.html", helper.resolveInternally("/en/william.html"));
	}

	@Test
	public void testSchemeAndHost() {
		expectRequestURI("/de/index.s1.html/ahdgasdasd");
		expectMappingPrefix("http://domain.de:8443", "/content");
		expectResourcePath("/content/de/index/jcr:content/teaser");

		ResourceResolverHelper helper = ResourceResolverHelperFactory.createHelper(request, true);

		Assert.assertEquals("/content/en/william.html", helper.resolveInternally("/en/william.html"));
	}

	@Test
	public void mangleNoNamespace() {
		String mangleNamespaces = ResourceResolverHelperFactory.mangleNamespaces("/content/index");
		Assert.assertEquals("/content/index", mangleNamespaces);
	}

	@Test
	public void mangleOneNamespace() {
		String mangleNamespaces = ResourceResolverHelperFactory.mangleNamespaces("/content/jcr:index/jcr:bla/xx");
		Assert.assertEquals("/content/_jcr_index/_jcr_bla/xx", mangleNamespaces);
	}

	@Test
	public void mangleLeadingNamespace() {
		String mangleNamespaces = ResourceResolverHelperFactory.mangleNamespaces("jcr:index/content");
		Assert.assertEquals("_jcr_index/content", mangleNamespaces);
	}

	@Test
	public void mangleTrailingNamespace() {
		String mangleNamespaces = ResourceResolverHelperFactory.mangleNamespaces("/content/jcr:index");
		Assert.assertEquals("/content/_jcr_index", mangleNamespaces);
	}

	private void expectResourcePath(String resourcePath) {
		Mockito.when(resource.getPath()).thenReturn(resourcePath);
	}

	private void expectMappingPrefix(String schemhost, String prefix) {
		Mockito.when(resolver.map(Mockito.any(HttpServletRequest.class), Mockito.anyString()))
				.then((InvocationOnMock invocation) -> {
					String path = (String) invocation.getArguments()[1];
					path = context.resourceResolver().map(path);
					if (path.startsWith(prefix)) {
						return schemhost + path.substring(prefix.length());
					}

					return schemhost + path.substring(prefix.length());
				});
	}

	private void expectRequestURI(String uri) {
		Mockito.when(request.getRequestURI()).thenReturn(uri);
	}

}
