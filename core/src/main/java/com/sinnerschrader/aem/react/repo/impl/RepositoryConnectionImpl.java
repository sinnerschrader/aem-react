package com.sinnerschrader.aem.react.repo.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.wcm.api.PageManager;
import com.sinnerschrader.aem.react.repo.RepositoryConnection;

public class RepositoryConnectionImpl implements RepositoryConnection {

	private ResourceResolver resourceResolver;

	public RepositoryConnectionImpl(ResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
	}

	@Override
	public void close() {
		resourceResolver.close();
	}

	@Override
	public PageManager getPageManager() {
		return getResourceResolver().adaptTo(PageManager.class);
	}

	@Override
	public ResourceResolver getResourceResolver() {
		return resourceResolver;
	}

	@Override
	public Session getSession() {
		return resourceResolver.adaptTo(Session.class);
	}

	@Override
	public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
		return getSession().getWorkspace().getObservationManager();
	}

}
