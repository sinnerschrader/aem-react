package com.sinnerschrader.aem.react.json;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceResolverHelper implements ResourceResolver {

	private String prefix;
	private ResourceResolver delegate;

	@Override
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> arg0) {
		return delegate.adaptTo(arg0);
	}

	@Override
	public ResourceResolver clone(Map<String, Object> arg0) throws LoginException {
		return delegate.clone(arg0);
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public void commit() throws PersistenceException {
		delegate.commit();
	}

	@Override
	public Resource create(Resource arg0, String arg1, Map<String, Object> arg2) throws PersistenceException {
		return delegate.create(arg0, arg1, arg2);
	}

	@Override
	public void delete(Resource arg0) throws PersistenceException {
		delegate.delete(arg0);
	}

	@Override
	public Iterator<Resource> findResources(String arg0, String arg1) {
		return delegate.findResources(arg0, arg1);
	}

	@Override
	public Object getAttribute(String arg0) {
		return delegate.getAttribute(arg0);
	}

	@Override
	public Iterator<String> getAttributeNames() {
		return delegate.getAttributeNames();
	}

	@Override
	public Iterable<Resource> getChildren(Resource arg0) {
		return delegate.getChildren(arg0);
	}

	@Override
	public String getParentResourceType(Resource arg0) {
		return delegate.getParentResourceType(arg0);
	}

	@Override
	public String getParentResourceType(String arg0) {
		return delegate.getParentResourceType(arg0);
	}

	@Override
	public Resource getResource(Resource arg0, String arg1) {
		return delegate.getResource(arg0, arg1);
	}

	@Override
	public Resource getResource(String arg0) {
		return delegate.getResource(arg0);
	}

	@Override
	public String[] getSearchPath() {
		return delegate.getSearchPath();
	}

	@Override
	public String getUserID() {
		return delegate.getUserID();
	}

	@Override
	public boolean hasChanges() {
		return delegate.hasChanges();
	}

	@Override
	public boolean hasChildren(Resource arg0) {
		return delegate.hasChildren(arg0);
	}

	@Override
	public Resource getParent(Resource child) {
		return delegate.getParent(child);
	}

	@Override
	public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
		return delegate.copy(srcAbsPath, destAbsPath);
	}

	@Override
	public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
		return delegate.move(srcAbsPath, destAbsPath);
	}

	@Override
	public boolean isLive() {
		return delegate.isLive();
	}

	@Override
	public boolean isResourceType(Resource arg0, String arg1) {
		return delegate.isResourceType(arg0, arg1);
	}

	@Override
	public Iterator<Resource> listChildren(Resource arg0) {
		return delegate.listChildren(arg0);
	}

	@Override
	public String map(HttpServletRequest arg0, String arg1) {
		return delegate.map(arg0, arg1);
	}

	@Override
	public String map(String arg0) {
		return delegate.map(arg0);
	}

	@Override
	public Iterator<Map<String, Object>> queryResources(String arg0, String arg1) {
		return delegate.queryResources(arg0, arg1);
	}

	@Override
	public void refresh() {
		delegate.refresh();
	}

	@Override
	public Resource resolve(HttpServletRequest arg0, String arg1) {
		return delegate.resolve(arg0, resolveInternally(arg1));
	}

	@Override
	public Resource resolve(HttpServletRequest arg0) {
		return delegate.resolve(arg0);
	}

	@Override
	public Resource resolve(String arg0) {
		return delegate.resolve(resolveInternally(arg0));
	}

	@Override
	public void revert() {
		delegate.revert();
	}

	public ResourceResolverHelper(String prefix, ResourceResolver delegate) {
		super();
		this.prefix = prefix;
		this.delegate = delegate;
	}

	public String resolveInternally(String uriPathOrUrl) {
		String uriPath;
		try {
			uriPath = new URL(uriPathOrUrl).getPath();
		} catch (Exception e) {
			uriPath = uriPathOrUrl;
		}
		if (uriPath.startsWith(this.prefix)) {
			return uriPath;
		}
		return this.prefix + uriPath;
	}

}
