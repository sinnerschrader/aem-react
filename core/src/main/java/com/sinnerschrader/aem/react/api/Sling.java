package com.sinnerschrader.aem.react.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.servlet.RequestDispatcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.sling.JsonObjectCreator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.components.IncludeOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.PrintWriterResponseWrapper;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.react.metrics.MetricsHelper;

/**
 * This class is exposed to the react components and provides the base
 * functionality to get access to content and wrapper elements.
 *
 * @author stemey
 *
 */
public class Sling {

	public static class ResourceIncludeOptions {
		public ResourceIncludeOptions() {

		}

		public void setSelectors(String selectors) {
			this.selectors = getStringArray(selectors);
		}

		public void setAddSelectors(String addSelectors) {
			this.addSelectors = getStringArray(addSelectors);
		}

		private String[] getStringArray(String value) {
			if (StringUtils.isNotEmpty(value)) {
				return value.split(",");
			}
			return null;
		}

		public String[] getSelectors() {
			return selectors;
		}

		public String[] getAddSelectors() {
			return addSelectors;
		}

		private String[] selectors;
		private String[] addSelectors;
		private String decorationTagName;

		public void setDecorationTagName(String decorationTagName) {
			this.decorationTagName = decorationTagName;
		}

		public String getDecorationTagName() {
			return decorationTagName;
		}
	}

	public static final String ATTRIBUTE_AEM_REACT_DIALOG = "AEM_REACT_DIALOG";

	public static class EditDialog {
		private static final Map<String, String> REACT_MAPPING = new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;

			{
				put("class", "className");
				put("for", "htmlFor");
			}
		};

		private EditDialog child;

		private Map<String, String> attributes = new HashMap<>();

		private String element;

		private String html;

		public String getElement() {
			return element;
		}

		public void setElement(String element) {
			this.element = element;
		}

		public Map<String, String> getAttributes() {
			return attributes;
		}

		public String getHtml() {
			return html;
		}

		public void setHtml(String html) {
			this.html = html;
		}

		public EditDialog getChild() {
			return child;
		}

		public void setChild(EditDialog child) {
			this.child = child;
		}

		public void addAttribute(String key, String value) {
			String reactKey = REACT_MAPPING.get(key);
			if (reactKey != null) {
				this.attributes.put(reactKey, value);
			} else {
				this.attributes.put(key, value);
			}
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(Sling.class);

	private ScriptContext context;

	private ObjectMapper mapper;

	public Sling(ScriptContext context) {
		super();
		this.context = context;
		this.mapper = new ObjectMapper();
	}

	/**
	 * get a resource
	 *
	 * @param path
	 * @param depth
	 * @return json string
	 */
	public String getResource(final String path, final Integer depth) {
		SlingHttpServletRequest request = (SlingHttpServletRequest) context.getBindings(ScriptContext.ENGINE_SCOPE)
				.get(SlingBindings.REQUEST);

		int actualDepth;
		try {
			if (depth == null || depth < 1) {
				actualDepth = -1;
			} else {
				actualDepth = depth.intValue();
			}

			Resource resource = request.getResourceResolver().resolve(request, path);
			if (resource == null) {
				return null;
			}
			return JsonObjectCreator.create(resource, actualDepth).toString();

		} catch (JSONException e) {
			throw new TechnicalException("could not get current resource", e);
		}

	}

	/**
	 * get the aem wrapper element
	 *
	 * @param path
	 * @param resourceType
	 * @return json string
	 */
	public String renderDialogScript(String path, String resourceType) {
		String html = this._includeResource(path, resourceType, new ResourceIncludeOptions(), true);
		Document document = Jsoup.parse(html);
		EditDialog editDialog = null;
		for (Node node : document.body().childNodes()) {
			if (node instanceof Element) {
				editDialog = parseEditDialog(node);

				for (Node child : node.childNodes()) {
					if (child instanceof Element) {
						EditDialog childDialog = parseEditDialog(child);
						editDialog.setChild(childDialog);
						childDialog.setHtml(((Element) child).html());
						break;
					}

				}
				break;
			}
		}
		if (editDialog == null) {
			return "null";
		}
		try {
			return mapper.writeValueAsString(editDialog);
		} catch (JsonProcessingException e) {
			LOG.error("cannot convert editdialog to json", e);
			return "null";
		}
	}

	/**
	 * get the html of an included component
	 *
	 * @param path
	 * @param resourceType
	 * @return
	 */
	public String includeResource(String path, String resourceType, String addSelectors, String selectors,
			String decorationTagName) {
		return MetricsHelper.getCurrent().timer("include", () -> {
			ResourceIncludeOptions options = new ResourceIncludeOptions();
			options.setAddSelectors(addSelectors);
			options.setSelectors(selectors);
			options.setDecorationTagName(decorationTagName);
			return this._includeResource(path, resourceType, options, false);

		});
	}

	/**
	 * get the current resource
	 *
	 * @param depth
	 * @return
	 */
	public String currentResource(int depth) {
		SlingHttpServletRequest request = (SlingHttpServletRequest) context.getBindings(ScriptContext.ENGINE_SCOPE)
				.get(SlingBindings.REQUEST);

		try {
			return JsonObjectCreator.create(request.getResource(), depth).toString();
		} catch (JSONException e) {
			throw new TechnicalException("could not get current resource", e);
		}
	}

	public String getUrl() {
		SlingHttpServletRequest request = (SlingHttpServletRequest) context.getBindings(ScriptContext.ENGINE_SCOPE)
				.get(SlingBindings.REQUEST);
		return request.getRequestURI();

	}

	public String getPagePath() {
		SlingHttpServletRequest request = (SlingHttpServletRequest) context.getBindings(ScriptContext.ENGINE_SCOPE)
				.get(SlingBindings.REQUEST);
		String pathInfo = request.getPathInfo();
		return pathInfo;

	}

	private EditDialog parseEditDialog(Node node) {
		EditDialog editDialog = new EditDialog();
		editDialog.setElement(node.nodeName());
		for (Attribute attribute : node.attributes().asList()) {
			editDialog.addAttribute(attribute.getKey(), attribute.getValue());
		}
		return editDialog;
	}

	private String normalizePath(SlingHttpServletRequest request, final String path) {
		String actualPath = path;
		if (!path.startsWith("/") && !path.startsWith("http")) {
			actualPath = request.getResource().getPath() + "/" + path;
		}
		return ResourceUtil.normalize(actualPath);
	}

	private String _includeResource(String path, String resourceType, ResourceIncludeOptions includeOptions,
			boolean dialog) {
		StringWriter out = new StringWriter();
		Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
		if (StringUtils.isEmpty(path)) {
			LOG.error("Script path cannot be empty");
		} else {
			SlingHttpServletResponse customResponse = new PrintWriterResponseWrapper(new PrintWriter(out),
					(SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE));
			SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);

			String script = normalizePath(request, path);

			Resource includeRes = request.getResourceResolver().resolve(request, script);
			if (includeRes instanceof NonExistingResource
					|| includeRes.isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING)) {
				includeRes = new SyntheticResource(request.getResourceResolver(), script, resourceType);
			}
			try {
				RequestDispatcherOptions opts = new RequestDispatcherOptions(null);
				if (StringUtils.isNotEmpty(resourceType)) {
					opts.setForceResourceType(resourceType);
				}
				if (dialog) {
					Object previousValue = request.getAttribute(ATTRIBUTE_AEM_REACT_DIALOG);
					request.setAttribute(ATTRIBUTE_AEM_REACT_DIALOG, true);
					IncludeOptions options = IncludeOptions.getOptions(request, true);
					options.forceEditContext(true);
					RequestDispatcher dispatcher = request.getRequestDispatcher(includeRes, opts);
					dispatcher.include(request, customResponse);
					if (previousValue == null) {
						request.removeAttribute(ATTRIBUTE_AEM_REACT_DIALOG);
					} else {
						request.setAttribute(ATTRIBUTE_AEM_REACT_DIALOG, previousValue);
					}

				} else {
					IncludeOptions options = IncludeOptions.getOptions(request, true);
					if (includeOptions.getDecorationTagName() != null) {
						options.setDecorationTagName(includeOptions.getDecorationTagName());
					}

					opts.setAddSelectors(StringUtils.join(includeOptions.getAddSelectors(), ","));
					opts.setReplaceSelectors(StringUtils.join(includeOptions.getSelectors(), ","));
					RequestDispatcher dispatcher = request.getRequestDispatcher(includeRes, opts);
					dispatcher.include(request, customResponse);
				}

			} catch (Exception e) {
				LOG.error("Failed to include resource {}", script, e);
			}
		}
		return out.toString();
	}

}
