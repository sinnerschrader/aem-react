package com.sinnerschrader.aem.react.repo;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.ResourceResolver;

public interface RepositoryConnection extends AutoCloseable {

	public com.day.cq.wcm.api.PageManager getPageManager();
	ResourceResolver getResourceResolver();

	public Session getSession();

	/**
	 * Re-declared without "throws Exception".
	 */
	@Override
	public void close();
	public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException;


}
