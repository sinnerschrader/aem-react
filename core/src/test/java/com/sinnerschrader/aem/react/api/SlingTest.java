package com.sinnerschrader.aem.react.api;

import java.io.IOException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.api.Sling.EditDialog;

@RunWith(MockitoJUnitRunner.class)
public class SlingTest {

	private static final String HTML = "<span>test</span>";

	@Mock
	private ScriptContext context;

	@Mock
	private SlingHttpServletRequest request;

	@Mock
	private SlingHttpServletResponse response;

	@Mock
	private RequestDispatcher dispatcher;

	@Mock
	private Bindings bindings;

	@Rule
	public SlingContext slingCtx = new SlingContext(ResourceResolverType.JCR_MOCK);

	@Mock
	private Resource resource;

	@Test
	public void testGetResource() throws JsonProcessingException, IOException {

		slingCtx.build().resource("/content/test", "name", "Willem").getCurrentParent();

		Mockito.when(context.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(bindings);
		Mockito.when(bindings.get(SlingBindings.REQUEST)).thenReturn(request);
		Mockito.when(request.getResourceResolver()).thenReturn(slingCtx.resourceResolver());

		Sling sling = new Sling(context);
		String data = sling.getResource("/content/test", 0);
		JsonNode tree = new ObjectMapper().readTree(data);
		Assert.assertEquals("Willem", tree.get("name").textValue());

	}

	@Test
	public void testCurrentResource() throws JsonProcessingException, IOException {

		slingCtx.build().resource("/content/test", "name", "Willem").getCurrentParent();

		Mockito.when(context.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(bindings);
		Mockito.when(bindings.get(SlingBindings.REQUEST)).thenReturn(request);
		Mockito.when(request.getResourceResolver()).thenReturn(slingCtx.resourceResolver());
		Mockito.when(request.getResource()).thenReturn(slingCtx.currentResource("/content/test"));

		Sling sling = new Sling(context);
		String data = sling.currentResource(0);
		JsonNode tree = new ObjectMapper().readTree(data);
		Assert.assertEquals("Willem", tree.get("name").textValue());

	}
	@Test
	public void testIncludeResource() throws JsonProcessingException, IOException, ServletException {

		slingCtx.build().resource("/content/test", "name", "Willem").getCurrentParent();

		includeResource(HTML, null, "s1,s2");

		Sling sling = new Sling(context);
		String data = sling.includeResource("/content/test", "/apps/test", null, "s1,s2", null);
		Assert.assertEquals(HTML.trim(), data.trim());

	}

	@Test
	public void testIncludeResourceDecoration() throws JsonProcessingException, IOException, ServletException {

		slingCtx.build().resource("/content/test", "name", "Willem").getCurrentParent();

		includeResource(HTML, "s1,s2", null);

		Sling sling = new Sling(context);
		String data = sling.includeResource("/content/test", "/apps/test", "s1,s2", null, "ul");
		Assert.assertEquals(HTML.trim(), data.trim());

	}

	private void includeResource(String html, String addSelectors, String replaceSelectors)
			throws ServletException, IOException {
		Mockito.when(context.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(bindings);
		Mockito.when(bindings.get(SlingBindings.REQUEST)).thenReturn(request);
		Mockito.when(bindings.get(SlingBindings.RESPONSE)).thenReturn(response);
		Mockito.when(request.getResourceResolver()).thenReturn(slingCtx.resourceResolver());
		Answer<RequestDispatcher> dispatcherAnswer = (InvocationOnMock mock) -> {
			RequestDispatcherOptions options = (RequestDispatcherOptions) mock.getArguments()[1];
			Assert.assertEquals(addSelectors, options.getAddSelectors());
			Assert.assertEquals(replaceSelectors, options.getReplaceSelectors());
			return dispatcher;
		};
		Mockito.when(
				request.getRequestDispatcher(Mockito.any(Resource.class), Mockito.any(RequestDispatcherOptions.class)))
				.thenAnswer(dispatcherAnswer);

		Answer<Void> answer = (InvocationOnMock mock) -> {
			ServletResponse resp = (ServletResponse) mock.getArguments()[1];
			resp.getWriter().println(html);
			return null;
		};
		Mockito.doAnswer(answer).when(dispatcher).include(Mockito.any(ServletRequest.class),
				Mockito.any(ServletResponse.class));
	}

	@Test
	public void testRenderDialog() throws JsonProcessingException, IOException, ServletException {

		String dialogHtml = "<div class=\"section\"><cq data-path=\"/content/test\" data-config=\"{&quot;path&quot;:&quot;/content/test&quot;}\"></cq></div>";

		EditDialog dialog = renderDialog(dialogHtml);

		Assert.assertEquals("div", dialog.getElement());
		Assert.assertEquals("section", dialog.getAttributes().get("className"));
		Assert.assertEquals("/content/test", dialog.getChild().getAttributes().get("data-path"));

	}

	@Test
	public void testRenderDialogWhitespace() throws JsonProcessingException, IOException, ServletException {

		String dialogHtml = "<div class=\"section\"> Test  <cq data-path=\"/content/test\" data-config=\"{&quot;path&quot;:&quot;/content/test&quot;}\"></cq></div>";

		EditDialog dialog = renderDialog(dialogHtml);

		Assert.assertEquals("div", dialog.getElement());
		Assert.assertEquals("section", dialog.getAttributes().get("className"));
		Assert.assertEquals("/content/test", dialog.getChild().getAttributes().get("data-path"));

	}

	@Test
	public void testRenderDialogNoMetadata() throws JsonProcessingException, IOException, ServletException {

		String dialogHtml = "<div class=\"section\"> Test  </div>";

		EditDialog dialog = renderDialog(dialogHtml);

		Assert.assertEquals("div", dialog.getElement());
		Assert.assertEquals("section", dialog.getAttributes().get("className"));
		Assert.assertNull(dialog.getChild());

	}

	@Test
	public void testRenderDialogEmpty() throws JsonProcessingException, IOException, ServletException {

		String dialogHtml = "";

		EditDialog dialog = renderDialog(dialogHtml);

		Assert.assertNull(dialog);

	}

	@Test
	public void testGetUrl() throws JsonProcessingException, IOException, ServletException {

		String expectedUrl = "/content/x";
		Mockito.when(context.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(bindings);
		Mockito.when(bindings.get(SlingBindings.REQUEST)).thenReturn(request);
		Mockito.when(request.getRequestURI()).thenReturn(expectedUrl);
		String url = createSling().getUrl();

		Assert.assertEquals(expectedUrl, url);

	}

	@Test
	public void testGetPagePath() throws JsonProcessingException, IOException, ServletException {

		String expectedPath="/content/x";
		Mockito.when(context.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(bindings);
		Mockito.when(bindings.get(SlingBindings.REQUEST)).thenReturn(request);
		Mockito.when(request.getPathInfo()).thenReturn(expectedPath);
		String path = createSling().getPagePath();

		Assert.assertEquals(expectedPath, path);

	}



	private EditDialog renderDialog(String dialogHtml)
			throws ServletException, IOException, JsonParseException, JsonMappingException {
		slingCtx.build().resource("/content/test", "name", "Willem").getCurrentParent();

		Mockito.when(context.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(bindings);
		Mockito.when(bindings.get(SlingBindings.REQUEST)).thenReturn(request);
		Mockito.when(bindings.get(SlingBindings.RESPONSE)).thenReturn(response);
		Mockito.when(request.getResourceResolver()).thenReturn(slingCtx.resourceResolver());
		Mockito.when(
				request.getRequestDispatcher(Mockito.any(Resource.class), Mockito.any(RequestDispatcherOptions.class)))
				.thenReturn(dispatcher);
		Answer<Void> answer = (InvocationOnMock mock) -> {
			ServletResponse resp = (ServletResponse) mock.getArguments()[1];
			resp.getWriter().println(dialogHtml);
			return null;
		};
		Mockito.doAnswer(answer).when(dispatcher).include(Mockito.any(ServletRequest.class),
				Mockito.any(ServletResponse.class));

		Sling sling1 = new Sling(context);
		Sling sling = sling1;
		String data = sling.renderDialogScript("/content/test", "/apps/test");
		EditDialog dialog = new ObjectMapper().readValue(data, EditDialog.class);
		return dialog;
	}

	private Sling createSling() {

		return new Sling(context);

	}

}
