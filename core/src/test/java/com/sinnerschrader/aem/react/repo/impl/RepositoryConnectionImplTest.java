package com.sinnerschrader.aem.react.repo.impl;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.day.cq.wcm.api.PageManager;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryConnectionImplTest {

	@Mock
	private ResourceResolver resourceResolver;

	@Mock
	private Session session;

	@Mock
	private PageManager pageManager;

	@Test
	public void getResourceResolver() {
		Assert.assertEquals(resourceResolver, new RepositoryConnectionImpl(resourceResolver).getResourceResolver());
	}

	@Test
	public void getSession() {
		Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
		Assert.assertEquals(session, new RepositoryConnectionImpl(resourceResolver).getSession());
	}

	@Test
	public void getPageManager() {
		Mockito.when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
		Assert.assertEquals(pageManager, new RepositoryConnectionImpl(resourceResolver).getPageManager());
	}

}
