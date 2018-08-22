package com.sinnerschrader.aem.react.node;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinnerschrader.aem.react.ReactScriptEngine.RenderResult;
import com.sinnerschrader.aem.react.api.Sling.EditDialog;
import com.sinnerschrader.aem.react.exception.TechnicalException;

public class NodeRenderer {

	private static final Logger LOGGER = LoggerFactory.getLogger(NodeRenderer.class);

	private Collection<String> supportedTypes;

	private String url;

	private HttpClient client;

	private ObjectMapper modelMapper;

	private EditDialogLoader editoDialogLoader;

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

			StringEntity entity = new StringEntity(json, ContentType.create("application/json", "UTF-8"));
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
			EditDialog editDialog = editoDialogLoader.load(path, resourceType);
			((ObjectNode) cache.get("wrapper")).set(path, modelMapper.convertValue(editDialog, ObjectNode.class));
		} else {
			LOGGER.warn("type is missing on model {}", path);
		}

	}

	public NodeRenderer(String url, HttpClient client, ObjectMapper modelMapper, EditDialogLoader editDialogLoader) {
		super();
		this.url = url;
		this.client = client;
		this.modelMapper = modelMapper;
	}

	public boolean supports(String resourceType) {
		return supportedTypes.contains(resourceType);
	}



}
