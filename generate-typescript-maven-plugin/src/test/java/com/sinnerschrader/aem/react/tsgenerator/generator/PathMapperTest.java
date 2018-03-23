package com.sinnerschrader.aem.react.tsgenerator.generator;

import org.junit.Assert;
import org.junit.Test;

import com.sinnerschrader.aem.react.tsgenerator.generator.PathMapper;

public class PathMapperTest {

	@Test
	public void testBelow() {
		String apply = new PathMapper("de.DD").apply("de.x.Model");
		Assert.assertEquals("./x/Model", apply);
	}

	@Test
	public void testAbove() {
		String apply = new PathMapper("de.x.DD").apply("de.Model");
		Assert.assertEquals("../Model", apply);
	}

}
