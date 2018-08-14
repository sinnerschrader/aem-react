package com.sinnerschrader.aem.react.tsgenerator.fromclass;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.TypeDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.generator.PathMapper;
import com.sinnerschrader.aem.react.tsgenerator.generator.TestModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.TestValue;

public class GeneratorFromClassTest {

	private PathMapper mapper = new PathMapper("de");

	@Test
	public void convertString() {
		TypeDescriptor type = GeneratorFromClass.convertType(String.class, null, mapper);

		Assert.assertEquals("string", type.getType());
		Assert.assertFalse(type.isArray());
	}

	@Test
	public void convertStringArray() {
		TypeDescriptor type = GeneratorFromClass.convertType(String[].class, null, mapper);

		Assert.assertEquals("string", type.getType());
		Assert.assertTrue(type.isArray());
	}

	@Test
	public void convertNumberArray() {
		TypeDescriptor type = GeneratorFromClass.convertType(int[].class, null, mapper);

		Assert.assertEquals("number", type.getType());
		Assert.assertTrue(type.isArray());
	}

	@Test
	public void convertComplex() {
		TypeDescriptor type = GeneratorFromClass.convertType(TestModel.class, null, mapper);

		Assert.assertEquals("TestModel", type.getType());
		Assert.assertFalse(type.isArray());

	}

	@Test
	public void convertComplexArray() {
		TypeDescriptor type = GeneratorFromClass.convertType(TestModel[].class, null, mapper);

		Assert.assertEquals("TestModel", type.getType());
		Assert.assertTrue(type.isArray());

	}

	@Test
	public void convertAnyArray() {
		TypeDescriptor type = GeneratorFromClass.convertType(Object[].class, new ClassBean("any"), mapper);

		Assert.assertEquals("any", type.getType());
		Assert.assertTrue(type.isArray());

	}

	@Test
	public void converComplexList() {
		TypeDescriptor type = GeneratorFromClass.convertType(List.class, createElement(TestModel.class), mapper);

		Assert.assertEquals("TestModel", type.getType());
		Assert.assertTrue(type.isArray());

	}

	@Test
	public void converComplexMap() {
		TypeDescriptor type = GeneratorFromClass.convertType(Map.class, createElement(TestModel.class), mapper);

		Assert.assertEquals("TestModel", type.getType());
		Assert.assertTrue(type.isMap());

	}

	@Test
	public void converPrimitiveList() {
		TypeDescriptor type = GeneratorFromClass.convertType(List.class, createElement(String.class), mapper);

		Assert.assertEquals("string", type.getType());
		Assert.assertTrue(type.isArray());

	}

	private ClassBean createElement(final Class<?> clazz) {
		return new ClassBean(clazz);
	}

	@Test
	public void converEnum() {
		EnumDescriptor type = GeneratorFromClass.createEnumDescriptor(TestValue.class);

		Assert.assertEquals("TestValue", type.getName());
		Assert.assertEquals(3, type.getValues().size());
		Assert.assertEquals("b", type.getValues().get(1));

	}
}
