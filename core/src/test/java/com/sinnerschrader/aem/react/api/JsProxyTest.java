package com.sinnerschrader.aem.react.api;

import org.junit.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsProxyTest {

	public static class Test {
		private String value;

		public String getValue() {
			return value;
		}

		public Test(String value) {
			super();
			this.value = value;
		}

		public int calculate(int a, int b) {
			return a + b;
		}
	}

	private ObjectMapper objectMapper = new ObjectMapper();

	@org.junit.Test
	public void testGetter() throws Exception {
		JsProxy proxy = new JsProxy(new Test("hallo"), Test.class, objectMapper);
		String json = proxy.get("value");
		Assert.assertEquals("\"hallo\"", json);
	}

	@org.junit.Test
	public void testgetObject() throws Exception {
		JsProxy proxy = new JsProxy(new Test("hallo"), Test.class, objectMapper);
		String json = proxy.getObject();
		Assert.assertEquals("{\"value\":\"hallo\"}", json);
	}

	@org.junit.Test
	public void testInvoke() throws Exception {
		JsProxy proxy = new JsProxy(new Test("hallo"), Test.class, objectMapper);
		String json = proxy.invoke("calculate", new Object[] { 1, 2 });
		Assert.assertEquals("3", json);
	}

}
