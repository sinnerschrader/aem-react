package com.sinnerschrader.aem.react.mapping;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

@SuppressWarnings("all")
public class ResourceResolverWrapper implements ResourceResolver{

	private ResourceResolver delegate;

	public ResourceResolverWrapper(ResourceResolver delegate) {
		super();
		this.delegate = delegate;
	}

	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		return delegate.adaptTo(type);
	}

	public Resource resolve(HttpServletRequest request, String absPath) {
		return delegate.resolve(request, absPath);
	}

	public Resource resolve(String absPath) {
		return delegate.resolve(absPath);
	}

	public Resource resolve(HttpServletRequest request) {
		return delegate.resolve(request);
	}

	public String map(String resourcePath) {
		return delegate.map(resourcePath);
	}

	public String map(HttpServletRequest request, String resourcePath) {
		return delegate.map(request, resourcePath);
	}

	public Resource getResource(String path) {
		return delegate.getResource(path);
	}

	public Resource getResource(Resource base, String path) {
		return delegate.getResource(base, path);
	}

	public String[] getSearchPath() {
		return delegate.getSearchPath();
	}

	public Iterator<Resource> listChildren(Resource parent) {
		return delegate.listChildren(parent);
	}

	public Resource getParent(Resource child) {
		return delegate.getParent(child);
	}

	public Iterable<Resource> getChildren(Resource parent) {
		return delegate.getChildren(parent);
	}

	public Iterator<Resource> findResources(String query, String language) {
		return delegate.findResources(query, language);
	}

	public Iterator<Map<String, Object>> queryResources(String query, String language) {
		return delegate.queryResources(query, language);
	}

	public boolean hasChildren(Resource resource) {
		return delegate.hasChildren(resource);
	}

	public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
		return delegate.clone(authenticationInfo);
	}

	public boolean isLive() {
		return delegate.isLive();
	}

	public void close() {
		delegate.close();
	}

	public String getUserID() {
		return delegate.getUserID();
	}

	public Iterator<String> getAttributeNames() {
		return delegate.getAttributeNames();
	}

	public Object getAttribute(String name) {
		return delegate.getAttribute(name);
	}

	public void delete(Resource resource) throws PersistenceException {
		delegate.delete(resource);
	}

	public Resource create(Resource parent, String name, Map<String, Object> properties) throws PersistenceException {
		return delegate.create(parent, name, properties);
	}

	public void revert() {
		delegate.revert();
	}

	public void commit() throws PersistenceException {
		delegate.commit();
	}

	public boolean hasChanges() {
		return delegate.hasChanges();
	}

	public String getParentResourceType(Resource resource) {
		return delegate.getParentResourceType(resource);
	}

	public String getParentResourceType(String resourceType) {
		return delegate.getParentResourceType(resourceType);
	}

	public boolean isResourceType(Resource resource, String resourceType) {
		return delegate.isResourceType(resource, resourceType);
	}

	public void refresh() {
		delegate.refresh();
	}

	public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
		return delegate.copy(srcAbsPath, destAbsPath);
	}

	public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
		return delegate.move(srcAbsPath, destAbsPath);
	}

}
