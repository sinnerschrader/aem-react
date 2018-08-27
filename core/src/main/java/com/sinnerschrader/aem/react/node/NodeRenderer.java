package com.sinnerschrader.aem.react.node;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Sling.EditDialog;
import com.sinnerschrader.aem.react.exception.TechnicalException;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@Slf4j
public class NodeRenderer {

	static class DummyEditLoader implements EditDialogLoader {

		static final DummyEditLoader INSTANCE = new DummyEditLoader();

		@Override
		public EditDialog load(String path, String resourceType) {
			return new EditDialog();
		}
	}

	private final Set<String> supportedTypes;
	private final String url;
	private final HttpClient client;
	private final ObjectMapper modelMapper;
	private EditDialogLoader editDialogLoader;

	public NodeRenderer(
			String url, String[] supportedTypes, HttpClient client, ObjectMapper modelMapper,
			EditDialogLoader editDialogLoader
	) {
		this(url, new HashSet<>(asList(supportedTypes)), client, modelMapper, editDialogLoader);
	}

	public NodeRenderer(
			String url, Set<String> supportedTypes, HttpClient client, ObjectMapper modelMapper,
			EditDialogLoader editDialogLoader
	) {
		this.url = requireNonNull(url);
		this.supportedTypes = requireNonNull(supportedTypes);
		this.client = requireNonNull(client);
		this.modelMapper = requireNonNull(modelMapper);
		this.editDialogLoader = editDialogLoader != null ? editDialogLoader : DummyEditLoader.INSTANCE;
	}

	public boolean supports(String resourceType) {
		return supportedTypes.contains(resourceType);
	}

	public RenderResult render(String path, String resourceType, String wcmmode, String[] selectors,
			Object cacheableModel) {

		try {
			ObjectNode requestNode = modelMapper.createObjectNode();
			requestNode.put("resourceType", resourceType);
			requestNode.put("path", path);
			requestNode.put("wcmmode", wcmmode);
			ArrayNode selectorsArray = requestNode.putArray("selectors");
			for (String selector : selectors) {
				selectorsArray.add(selector);
			}
			ObjectNode cache = requestNode.putObject("cache");

			cache.putObject("transforms");
			cache.putObject("wrappers");
			ObjectNode model = modelMapper.convertValue(cacheableModel, ObjectNode.class);

			populateCache(path, model, cache);
			String json = modelMapper.writeValueAsString(requestNode);

			StringEntity entity = new StringEntity(json, ContentType.create("application/json", StandardCharsets.UTF_8));
			HttpPost httppost = new HttpPost(url);
			httppost.setEntity(entity);
			HttpResponse response = client.execute(httppost);
			InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(),
					response.getEntity().getContentEncoding().getValue());
			long length = response.getEntity().getContentLength();
			char[] buffer = new char[(int) length];
			IOUtils.readFully(reader, buffer);
			String html = new String(buffer);

			RenderResult result = new RenderResult();
			result.cache = modelMapper.writeValueAsString(cache);
			result.html = html;

			return result;

		} catch (Exception e) {
			throw new TechnicalException("cannot render in node", e);
		}
	}

	private void populateCache(String path, ObjectNode model, ObjectNode cache) {
		// JsonNode itemsOrder = model.get(":itemsOrder");

		if (model.hasNonNull(":items")) {
			JsonNode items = model.get(":items");
			Iterator<Entry<String, JsonNode>> fields = items.fields();
			while (fields.hasNext()) {
				Entry<String, JsonNode> entry = fields.next();
				populateCache(path + "/" + entry.getKey(), (ObjectNode) entry.getValue(), cache);

			}
			model.remove(":items");
		}

		((ObjectNode) cache.get("transforms")).set(path, model);

		if (model.has(":type")) {
			JsonNode typeNode = model.get(":type");
			String resourceType = typeNode.asText();
			EditDialog editDialog = editDialogLoader.load(path, resourceType);
			ObjectNode value = modelMapper.convertValue(editDialog, ObjectNode.class);
			ObjectNode wrapper = (ObjectNode) cache.get("wrapper");
			wrapper.set(path, value);
		} else {
			LOG.warn("type is missing on model {}", path);
		}
	}

}
