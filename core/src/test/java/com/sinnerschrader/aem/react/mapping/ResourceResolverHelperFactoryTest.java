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

	@Mock
	private Resource resolvedResource;

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
	public void test() {
		expectRequestURI("/de/index.s1.html/ahdgasdasd");
		expectMappingPrefix("", "/content");
		expectResourcePath("/content/de/index/jcr:content/teaser");
		Mockito.when(resolver.resolve("/content/en/william.html")).thenReturn(resolvedResource);

		ResourceResolver resolver = ResourceResolverHelperFactory.create(request, true);

		Assert.assertEquals(resolvedResource, resolver.resolve("/en/william.html"));
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
	public void testSlingAliasHomePage() {
		Mockito.when(resolver.map(Mockito.any(HttpServletRequest.class), Mockito.anyString()))
				.thenReturn("/alias_de/alias_x/jcr:content/teaser");
		expectResourcePath("/content/sample/de/x/jcr:content/teaser");

		ResourceResolverHelper helper = ResourceResolverHelperFactory.createHelper(request, true);

		// although this is wrong. The important part is the prefix.
		Assert.assertEquals("/content/sample/alias_de/william.html", helper.resolveInternally("/aliased_de/william.html"));
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

	@Test
	public void testGetPathPrefix() throws Exception {
		String pathPrefix = ResourceResolverHelperFactory.getPathPrefix("/a/b/c/d", "/c/d");
		Assert.assertEquals("/a/b", pathPrefix);
	}

	@Test
	public void testGetPathPrefixWithAlias() throws Exception {
		String pathPrefix = ResourceResolverHelperFactory.getPathPrefix("/a/b/c/d", "/x123123123/y12");
		Assert.assertEquals("/a/b", pathPrefix);
	}

	@Test
	public void testGetPathPrefixWithAliasReverse() throws Exception {
		String pathPrefix = ResourceResolverHelperFactory.getPathPrefix("/x123123123/y12", "/a/b/c/d");
		Assert.assertEquals("/a/b", pathPrefix);
	}

	@Test
	public void testGetPathPrefixNone() throws Exception {
		String pathPrefix = ResourceResolverHelperFactory.getPathPrefix("/x123123123/y12", "/a/b");
		Assert.assertEquals("", pathPrefix);
	}
}
