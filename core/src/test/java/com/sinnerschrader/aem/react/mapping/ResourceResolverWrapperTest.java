package com.sinnerschrader.aem.react.mapping;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.mockito.Mockito;

public class ResourceResolverWrapperTest {

	public static class Model {

	}

	private ResourceResolver resolver = new ResourceResolverWrapper(Mockito.mock(ResourceResolver.class));

	@Test
	public void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (Method method : ResourceResolver.class.getDeclaredMethods()) {
			method.invoke(resolver, new Object[method.getParameterTypes().length]);
		}
	}

}
