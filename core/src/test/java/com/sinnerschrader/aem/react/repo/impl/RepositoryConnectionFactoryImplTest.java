package com.sinnerschrader.aem.react.repo.impl;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sinnerschrader.aem.react.repo.RepositoryConnection;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryConnectionFactoryImplTest {

	@Mock
	private ResourceResolverFactory resourceResolverFactory;

	@InjectMocks
	private RepositoryConnectionFactoryImpl factory;

	@Test
	public void testWithSubserviceName() {
		RepositoryConnection con = factory.getConnection("test");
		Assert.assertNotNull(con);
	}

	@Test
	public void test() {
		RepositoryConnection con = factory.getConnection(null);
		Assert.assertNotNull(con);
	}

}
