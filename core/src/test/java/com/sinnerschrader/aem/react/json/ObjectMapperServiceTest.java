package com.sinnerschrader.aem.react.json;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;

import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class ObjectMapperServiceTest {

	public static class TestModel {
		public TestModel(String content) {
			super();
			this.content = content;
		}

		private String content;

		public String getContent() {
			return content;
		}
	}

	@Mock
	private ComponentContext ctx;

	@Test
	public void testActivate() {
		Dictionary props = new Hashtable<>();
		props.put(ObjectMapperService.JSON_RESOURCEMAPPING_INCLUDE_PATTERN, "^/content");
		props.put(ObjectMapperService.JSON_RESOURCEMAPPING_EXCLUDE_PATTERN, "^/content/dam");
		Mockito.when(ctx.getProperties()).thenReturn(props);
		ObjectMapperService service = new ObjectMapperService();
		service.activate(ctx, null);
	}

	@Test
	public void testWrite() {
		Dictionary props = new Hashtable<>();
		props.put(ObjectMapperService.JSON_RESOURCEMAPPING_INCLUDE_PATTERN, "^/content");
		props.put(ObjectMapperService.JSON_RESOURCEMAPPING_EXCLUDE_PATTERN, "^/content/dam");
		Mockito.when(ctx.getProperties()).thenReturn(props);
		ObjectMapperService service = new ObjectMapperService();
		service.activate(ctx, null);
		String value = service.write(new TestModel("hi"), new MockSlingHttpServletRequest());
		Assert.assertEquals("{\"content\":\"hi\"}", value);
	}

}
