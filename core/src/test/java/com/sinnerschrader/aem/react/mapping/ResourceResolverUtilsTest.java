package com.sinnerschrader.aem.react.mapping;

import org.junit.Assert;
import org.junit.Test;

import com.sinnerschrader.aem.react.mapping.ResourceResolverUtils;

public class ResourceResolverUtilsTest {

	@Test
	public void testRelativePath() {
		String path = ResourceResolverUtils.getUriPath("content/test");
		Assert.assertEquals("content/test", path);
	}

	@Test
	public void testAbsolutePath() {
		String path = ResourceResolverUtils.getUriPath("/content/test");
		Assert.assertEquals("/content/test", path);
	}

	@Test
	public void testAbsoluteUrl() {
		String path = ResourceResolverUtils.getUriPath("http://localhost:888/content/test");
		Assert.assertEquals("/content/test", path);
	}

}
