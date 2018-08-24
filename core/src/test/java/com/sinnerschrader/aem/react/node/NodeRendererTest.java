package com.sinnerschrader.aem.react.node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;

@RunWith(MockitoJUnitRunner.class)
public class NodeRendererTest {

	public interface ResultAssertion {
		void assertValue(ObjectNode cache) throws Exception;
	}

	@Mock
	private HttpClient client;

	@Mock
	private HttpResponse response;

	@Mock
	private HttpEntity entity;

	@Mock
	private Header contentEncodingHeader;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private EditDialogLoader editDialogLoader;

	@Test
	public void renderLeaf() throws Exception {

		String html = "<html>";

		ArgumentCaptor<HttpPost> postCaptor = createMocks(html);

		final String resourceType = "component/test";
		NodeRenderer nodeRenderer = new NodeRenderer("/", Collections.singleton(resourceType), client, objectMapper, editDialogLoader);
		TestModel cacheableModel = new TestModel();
		cacheableModel.setText("Moin");
		RenderResult renderResult = nodeRenderer.render("/content", resourceType, "disabled", new String[0],
				cacheableModel);

		Assert.assertEquals(html, renderResult.html);

		assertResult(postCaptor, (ObjectNode post) -> {
			Assert.assertEquals(resourceType, post.get("resourceType").textValue());
			Assert.assertEquals("disabled", post.get("wcmmode").textValue());
			Assert.assertEquals("/content", post.get("path").textValue());
			JsonNode cache = post.get("cache");
			JsonNode transforms = cache.get("transforms");
			JsonNode transform = transforms.get("/content");
			Assert.assertEquals("Moin", transform.get("text").textValue());

			Assert.assertEquals(objectMapper.writeValueAsString(cache), renderResult.cache);
		});
	}

	@Test
	public void renderSubTree() throws Exception {
		String html = "<html>";

		ArgumentCaptor<HttpPost> postCaptor = createMocks(html);

		final String resourceType = "component/test";
		NodeRenderer nodeRenderer = new NodeRenderer("/",  Collections.singleton(resourceType), client, objectMapper, editDialogLoader);
		TestModel testModel = new TestModel();
		testModel.setText("Moin");
		CompoundModel cacheableModel = new CompoundModel();
		cacheableModel.setTestModel(testModel);
		cacheableModel.setHeadline("Headline");
		RenderResult renderResult = nodeRenderer.render("/content", resourceType, "disabled", new String[0],
				cacheableModel);

		Assert.assertEquals(html, renderResult.html);

		assertResult(postCaptor, (ObjectNode post) -> {
			Assert.assertEquals(resourceType, post.get("resourceType").textValue());
			Assert.assertEquals("disabled", post.get("wcmmode").textValue());
			Assert.assertEquals("/content", post.get("path").textValue());
			JsonNode cache = post.get("cache");
			JsonNode transforms = cache.get("transforms");
			JsonNode transform = transforms.get("/content");
			Assert.assertEquals("Headline", transform.get("headline").textValue());

			JsonNode testTransform = transforms.get("/content/test");
			Assert.assertEquals("Moin", testTransform.get("text").textValue());

			Assert.assertEquals(objectMapper.writeValueAsString(cache), renderResult.cache);
		});
	}

	private void assertResult(ArgumentCaptor<HttpPost> postCaptor, ResultAssertion assertion) throws Exception {
		Mockito.verify(client, Mockito.atLeastOnce()).execute(postCaptor.capture());
		HttpPost httpPost = postCaptor.getValue();
		try (InputStream content = httpPost.getEntity().getContent()) {
			ObjectNode data = (ObjectNode) objectMapper.readTree(content);
			assertion.assertValue(data);
		}
	}

	private ArgumentCaptor<HttpPost> createMocks(String html) throws IOException {
		Mockito.when(response.getEntity()).thenReturn(entity);
		Mockito.when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
		Mockito.when(entity.getContentLength()).thenReturn((long) html.length());
		Mockito.when(entity.getContentEncoding()).thenReturn(contentEncodingHeader);
		Mockito.when(contentEncodingHeader.getValue()).thenReturn("UTF-8");
		ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);
		Mockito.when(client.execute(Matchers.any(HttpPost.class))).thenReturn(response);
		return postCaptor;
	}
}
