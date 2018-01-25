package com.sinnerschrader.aem.react.json;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.ReactScriptEngineFactory;
import com.sinnerschrader.aem.react.exception.TechnicalException;
import com.sinnerschrader.aem.reactapi.json.JsonService;

@Component(immediate = true, metatype = true)
@Service(JsonService.class)
@Properties({ //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_INCLUDE_PATTERN, label = "pattern for text properties in sling models that must be mapped by resource resover", value = "^/content"), //
		@Property(name = ReactScriptEngineFactory.JSON_RESOURCEMAPPING_EXCLUDE_PATTERN, label = "pattern to include properties from resource mapping") //
})

public class ObjectMapperService implements JsonService {
	public static final String JSON_RESOURCEMAPPING_INCLUDE_PATTERN = "json.resourcemapping.include.pattern";
	public static final String JSON_RESOURCEMAPPING_EXCLUDE_PATTERN = "json.resourcemapping.exclude.pattern";
	private ObjectMapper objectMapper;

	@Activate
	public void activate(final ComponentContext context, Map<String, Object> properties) {
		String includePattern = PropertiesUtil
				.toString(context.getProperties().get(JSON_RESOURCEMAPPING_INCLUDE_PATTERN), "^/content");
		String excludePattern = PropertiesUtil
				.toString(context.getProperties().get(JSON_RESOURCEMAPPING_EXCLUDE_PATTERN), null);
		this.objectMapper = new ObjectMapperFactory().create(includePattern, excludePattern);
	}

	@Override
	public String write(Object value, SlingHttpServletRequest request) {
		StringWriter writer = new StringWriter();
		boolean remove = false;
		try {
			remove = ResourceMapperLocator.setInstance(request);
			objectMapper.writeValue(writer, value);
		} catch (IOException e) {
			throw new TechnicalException("cannot convert object to json", e);
		} finally {
			if (remove) {
				ResourceMapperLocator.clearInstance();
			}
		}
		return writer.toString();
	}

}
