package com.sinnerschrader.aem.react.mapping;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sinnerschrader.aem.react.mapping.ResourceResolverHelper;

@RunWith(MockitoJUnitRunner.class)
public class ResourceResolverHelperTest {

	@Mock
	private ResourceResolver delegate;

	@Before
	public void setup() {
		Mockito.when(delegate.resolve(Mockito.anyString())).thenAnswer((InvocationOnMock invoke) -> {
			return new MockResource((String) invoke.getArguments()[0], null, null);
		});
	}

	@Test
	public void test() {

		ResourceResolverHelper helper = new ResourceResolverHelper("/content", delegate);

		Assert.assertEquals("/content/index", helper.resolve("/index").getPath());
	}

	@Test
	public void testNoPrefix() {

		ResourceResolverHelper helper = new ResourceResolverHelper("", delegate);

		Assert.assertEquals("/index", helper.resolve("/index").getPath());
	}

	@Test
	public void testProtocol() {

		ResourceResolverHelper helper = new ResourceResolverHelper("/content", delegate);

		Assert.assertEquals("/content/other/index", helper.resolve("http://www.domain.de/other/index").getPath());
	}
}
