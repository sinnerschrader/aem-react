package com.sinnerschrader.aem.react.demo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.foundation.Download;

@Model(adaptables = SlingHttpServletRequest.class)
public class CityFinderModel {

	private static final Logger LOGGER = LoggerFactory.getLogger(CityFinderModel.class);

	@Self
	private SlingHttpServletRequest request;

	@Inject
	private ResourceResolver resourceResolver;

	public List<City> findCities(String basePath, String relPath) {
		List<City> cities = new ArrayList<>();
		Resource resource = resourceResolver.resolve(request, basePath);

		Page page = resourceResolver.adaptTo(PageManager.class).getContainingPage(resource);
		if (page != null) {
			Iterator<Page> cityPages = page.listChildren();
			while (cityPages.hasNext()) {
				Page cityPage = cityPages.next();
				ValueMap cityProps = cityPage.getContentResource().getValueMap();
				City city = new City();
				city.name = cityProps.get("jcr:title", String.class);
				city.id = cityPage.getName();
				Resource cityView = cityPage.getContentResource().getChild(relPath);
				if (cityView != null) {
					Resource image = cityView.getChild("image");
					if (image != null) {
						Download download = new Download(image);
						city.imageSrc = download.getHref();
					}
				} else {
					LOGGER.error("cannot find city view relative to page {}", relPath);
				}

				cities.add(city);

			}
		} else {
			LOGGER.error("cannot find base page at path: rel={}, base={}", relPath, basePath);
		}
		return cities;

	}
}
