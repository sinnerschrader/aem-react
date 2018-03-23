package com.sinnerschrader.aem.react.tsgenerator.generator;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ScanContext;
import com.sinnerschrader.aem.react.tsgenerator.fromclass.DiscriminatorPreprocessor;
import com.sinnerschrader.aem.react.tsgenerator.fromclass.GeneratorFromClass;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.FieldModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.InterfaceModel;

public class TypeScriptGeneratorTest {

	@Test
	public void testSimple() {
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(TestModel.class, new ScanContext(),
				new PathMapper(TestModel.class.getName()));
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals("TestModel", generate.getName());
		Assert.assertEquals(TestModel.class.getName(), generate.getFullSlingModelName());
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("value", field.getName());
		Assert.assertEquals("string", field.getTypes()[0]);
		Assert.assertEquals(0, generate.getImports().size());
		Assert.assertEquals("string", field.getTypes()[0]);
	}

	@Test
	public void testComplex() {
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(TestComplexModel.class, new ScanContext(),
				new PathMapper(TestComplexModel.class.getName()));
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("model", field.getName());
		Assert.assertEquals("TestModel", field.getTypes()[0]);
		Assert.assertEquals(1, generate.getImports().size());
		Assert.assertEquals("TestModel", generate.getImports().iterator().next().getName());
	}

	@Test
	public void testArray() {
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(TestArrayModel.class, new ScanContext(),
				new PathMapper(TestArrayModel.class.getName()));
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("models", field.getName());
		Assert.assertEquals("TestModel[]", field.getTypes()[0]);
		Assert.assertEquals(1, generate.getImports().size());
		Assert.assertEquals("TestModel", generate.getImports().iterator().next().getName());
	}

	@Test
	public void testList() {
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(TestListModel.class, new ScanContext(),
				new PathMapper(TestListModel.class.getName()));
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("models", field.getName());
		Assert.assertEquals("TestModel[]", field.getTypes()[0]);
		Assert.assertEquals(1, generate.getImports().size());
		Assert.assertEquals("TestModel", generate.getImports().iterator().next().getName());
	}

	@Test
	public void testListAnnotationOnGetter() {
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(TestGetterListModel.class,
				new ScanContext(), new PathMapper(TestGetterListModel.class.getName()));
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("models", field.getName());
		Assert.assertEquals("TestModel[]", field.getTypes()[0]);
		Assert.assertEquals(1, generate.getImports().size());
		Assert.assertEquals("TestModel", generate.getImports().iterator().next().getName());
	}

	@Test
	public void testMap() {
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(TestMapModel.class, new ScanContext(),
				new PathMapper(TestMapModel.class.getName()));
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("models", field.getName());
		Assert.assertEquals("{[key: string]: TestModel}", field.getTypes()[0]);
		Assert.assertEquals(1, generate.getImports().size());
		Assert.assertEquals("TestModel", generate.getImports().iterator().next().getName());
	}

	@Test
	public void testEnum() {
		EnumDescriptor descriptor = GeneratorFromClass.createEnumDescriptor(TestValue.class);
		String enumStr = TypeScriptGenerator.builder().build().generateEnum(descriptor);
		Assert.assertEquals("export type TestValue = 'a' | 'b' | 'c';\n", enumStr);

	}

	@Test
	public void testSubclassing() {
		ScanContext ctx = new ScanContext();
		DiscriminatorPreprocessor.findDiscriminators(BaseModel.class, new PathMapper(BaseModel.class.getName()), ctx);

		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(Sub1.class, ctx,
				new PathMapper(Sub1.class.getName()));
		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);

		Assert.assertEquals("BaseBaseModel", descriptor.getSuperClass().getType());
		Assert.assertEquals(1, model.getImports().size());
		Assert.assertEquals("BaseBaseModel", model.getSuperclass());
		Assert.assertNotNull(descriptor.getDiscriminator());
		Assert.assertNotNull(model.getDiscriminator());
		Assert.assertEquals("kind", model.getDiscriminator().getField());
		Assert.assertEquals("sub1", model.getDiscriminator().getValue());
		Assert.assertNotNull(model.getSuperclass());
		Assert.assertEquals(1, model.getFields().size());
	}

	@Test
	public void testDeepSubclassing() {
		ScanContext ctx = new ScanContext();
		DiscriminatorPreprocessor.findDiscriminators(BaseModel.class, new PathMapper(BaseModel.class.getName()), ctx);

		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(Sub2.class, ctx,
				new PathMapper(Sub2.class.getName()));
		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);

		Assert.assertEquals("BaseBaseModel", descriptor.getSuperClass().getType());
		Assert.assertEquals(1, model.getImports().size());
		Assert.assertEquals("BaseBaseModel", model.getSuperclass());
		Assert.assertNull(descriptor.getDiscriminator());
		Assert.assertNull(model.getDiscriminator());

		ClassDescriptor descriptor2 = GeneratorFromClass.createClassDescriptor(Sub3.class, ctx,
				new PathMapper(Sub3.class.getName()));
		InterfaceModel model2 = TypeScriptGenerator.builder().build().generateModel(descriptor2);

		Assert.assertNotNull(descriptor2.getDiscriminator());
		Assert.assertNotNull(model2.getDiscriminator());
		Assert.assertEquals("Sub2", descriptor2.getSuperClass().getType());

		Assert.assertEquals(1, model2.getImports().size());
		Assert.assertEquals("Sub2", model2.getSuperclass());
		Assert.assertEquals("kind", model2.getDiscriminator().getField());
		Assert.assertEquals("sub3", model2.getDiscriminator().getValue());
		Assert.assertNotNull(model2.getSuperclass());
		Assert.assertEquals(1, model2.getFields().size());
	}

	@Test
	public void testUnionType() {
		ScanContext ctx = new ScanContext();
		DiscriminatorPreprocessor.findDiscriminators(BaseModel.class, new PathMapper(BaseModel.class.getName()), ctx);
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(BaseModel.class, ctx,
				new PathMapper(BaseModel.class.getName()));
		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertNotNull(descriptor.getUnionType());
		Assert.assertNotNull(model.getUnionModel());
		Assert.assertEquals("kind", model.getUnionModel().getField());
	}

	@Test
	public void testTypeDiscriminatorJson() throws IOException {

		Sub1 sub1 = new Sub1();
		sub1.setMore("hi");
		StringWriter writer = new StringWriter();

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writeValue(writer, sub1);
		Assert.assertEquals("{\"kind\":\"sub1\",\"value\":null,\"more\":\"hi\"}", writer.toString());
	}

}
