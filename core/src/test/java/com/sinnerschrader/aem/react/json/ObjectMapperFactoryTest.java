package com.sinnerschrader.aem.react.json;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.reactapi.json.NoResourceMapping;

@RunWith(MockitoJUnitRunner.class)
public class ObjectMapperFactoryTest {

	private static class Model {

		public Model() {
		}

		public Model(String text) {
			super();
			this.text = text;
		}

		@NoResourceMapping
		private String text;

		public String getText() {
			return text;
		}

	}

	@Mock
	private ResourceMapper resourceMapper;

	@Test
	public void testUrlRewrite() throws JsonGenerationException, JsonMappingException, IOException {
		assertRewriting("/content/sample/de/de/home", "/de/de/home");
	}

	@Test
	public void testDamUrlRewrite() throws JsonGenerationException, JsonMappingException, IOException {
		assertRewriting("/content/dam/image", "/content/dam/image");
	}

	@Test
	public void testNoRewrite() throws JsonGenerationException, JsonMappingException, IOException {
		assertRewriting("wonderful richtext", "wonderful richtext");
	}

	@Test
	public void testAnnotation() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapperFactory().create("^/content", "^/content/dam");
		StringWriter writer = new StringWriter();

		String text = "/content/sample/de/de";
		Model model = new Model(text);
		ResourceMapperLocator.setInstance(resourceMapper);
		try {
			mapper.writeValue(writer, model);
		} finally {
			ResourceMapperLocator.clearInstance();
		}
		JsonNode resultingJson = mapper.readTree(writer.toString());
		Assert.assertEquals(text, resultingJson.get("text").asText());

	}

	private void assertRewriting(String url, String convertedUrl)
			throws IOException, JsonGenerationException, JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapperFactory().create("^/content", "^/content/dam");
		StringWriter writer = new StringWriter();
		Map<String, String> json = new HashMap<>();

		json.put("url", url);
		Mockito.when(resourceMapper.map(Mockito.eq(url))).thenReturn(convertedUrl);
		ResourceMapperLocator.setInstance(resourceMapper);
		try {
			mapper.writeValue(writer, json);
		} finally {
			ResourceMapperLocator.clearInstance();
		}
		JsonNode resultingJson = mapper.readTree(writer.toString());
		Assert.assertEquals(convertedUrl, resultingJson.get("url").asText());
	}

}
