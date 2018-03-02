package com.sinnerschrader.aem.react.loader;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.repo.RepositoryConnection;
import com.sinnerschrader.aem.react.repo.RepositoryConnectionFactory;

/**
 *
 * Listens to changes to a set of resources.
 *
 * @author stemey
 *
 */
public class JcrResourceChangeListener implements EventListener {

	public interface Listener {
		/**
		 * a resource was modified
		 *
		 * @param script
		 */
		void changed(String script);
	}

	private static final Logger LOG = LoggerFactory.getLogger(JcrResourceChangeListener.class);

	protected RepositoryConnectionFactory repositoryConnectionFactory;

	private Listener listener;

	private String subServiceName;

	public JcrResourceChangeListener(RepositoryConnectionFactory repositoryConnectionFactory, Listener listener,
			String subServiceName) {
		super();
		this.repositoryConnectionFactory = repositoryConnectionFactory;
		this.listener = listener;
		this.subServiceName = subServiceName;
	}

	private RepositoryConnection repositoryConnection;

	private ObservationManager observationManager;

	public void modified(String[] paths) throws RepositoryException {
		deactivate();
		activate(paths);
	}

	public void activate(String[] paths) {
		this.repositoryConnection = this.repositoryConnectionFactory.getConnection(subServiceName);
		try {
			this.observationManager = this.repositoryConnection.getObservationManager();

			final String[] types = { "nt:unstructured", "nt:resource" };
			final int eventTypes = Event.PROPERTY_CHANGED | Event.NODE_ADDED | Event.PROPERTY_ADDED;

			for (String path : paths) {
				this.observationManager.addEventListener(this, eventTypes, path, true, null, types, false);

			}
			LOG.info("Observing property changes to nodes under {}", (Object[]) paths);
		} catch (final RepositoryException e) {
			throw new TechnicalException("cannot actiate Preview PreviewReplicationService", e);
		}
	}

	public void deactivate() throws RepositoryException {
		if (this.observationManager != null) {
			this.observationManager.removeEventListener(this);
			this.observationManager = null;
		}
		if (this.repositoryConnection != null) {
			this.repositoryConnection.close();
			this.repositoryConnection = null;
		}
	}

	@Override
	public void onEvent(EventIterator itr) {
		try {
			this.listener.changed(itr.nextEvent().getPath());
		} catch (RepositoryException e) {
			LOG.error("error when observing resource", e);
		}
	}

}
