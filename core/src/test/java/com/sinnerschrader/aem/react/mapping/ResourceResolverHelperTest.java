package com.sinnerschrader.aem.react.mapping;

import org.apache.sling.api.resource.NonExistingResource;
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

@RunWith(MockitoJUnitRunner.class)
public class ResourceResolverHelperTest {

	@Mock
	private ResourceResolver delegate;

	@Before
	public void setup() {
	}

	private void expect(String path, boolean exists) {
		Mockito.when(delegate.resolve(path)).thenAnswer((InvocationOnMock invoke) -> {
			return exists ? new MockResource(path, null, null) : new NonExistingResource(null,path);
		});
	}

	@Test
	public void test() {

		expect("/index", false);
		expect("/content/index", true);
		ResourceResolverHelper helper = new ResourceResolverHelper("/content", delegate);

		Assert.assertEquals("/content/index", helper.resolve("/index").getPath());
	}

	@Test
	public void testUrlUTF8EncodedPath() {

		expect("/content/töst tüst", true);
		ResourceResolverHelper helper = new ResourceResolverHelper("/content", delegate);

		Assert.assertEquals("/content/töst tüst", helper.resolve("/t%C3%B6st%20t%C3%BCst").getPath());
	}

	@Test
	public void testNoPrefix() {

		expect("/index", true);
		ResourceResolverHelper helper = new ResourceResolverHelper("", delegate);

		Assert.assertEquals("/index", helper.resolve("/index").getPath());
	}

	@Test
	public void testProtocol() {

		expect("http://www.domain.de/other/index", false);
		expect("/content/other/index", true);
		ResourceResolverHelper helper = new ResourceResolverHelper("/content", delegate);

		Assert.assertEquals("/content/other/index", helper.resolve("http://www.domain.de/other/index").getPath());
	}

	@Test
	public void testImage() {

		expect("/content/dam/index", true);
		ResourceResolverHelper helper = new ResourceResolverHelper("/content", delegate);

		Assert.assertEquals("/content/dam/index", helper.resolve("/content/dam/index").getPath());
	}
}
