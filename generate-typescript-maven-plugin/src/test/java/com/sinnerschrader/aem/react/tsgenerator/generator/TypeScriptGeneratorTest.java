package com.sinnerschrader.aem.react.tsgenerator.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import com.adobe.cq.export.json.ContainerExporter;
import com.sinnerschrader.aem.react.tsgenerator.maven.TsElementDefault;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ScanContext;
import com.sinnerschrader.aem.react.tsgenerator.fromclass.DiscriminatorPreprocessor;
import com.sinnerschrader.aem.react.tsgenerator.fromclass.GeneratorFromClass;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.FieldModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.ImportModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.InterfaceModel;

public class TypeScriptGeneratorTest {

	private static ClassDescriptor createClassDescriptor(Class<?> clazz) {
		return createClassDescriptor(clazz, null);
	}

	private static ClassDescriptor createClassDescriptor(Class<?> clazz, String baseTsPath) {
		return createClassDescriptor(clazz, clazz.getPackage().getName(), baseTsPath);
	}

	private static ClassDescriptor createClassDescriptor(Class<?> clazz, String basePackage, String relativeTsPath) {
		return GeneratorFromClass.createClassDescriptor(clazz, new ScanContext(), null, new PathMapper(clazz.getName()),
				new TsPathMapper(clazz.getName(), basePackage, relativeTsPath));
	}

	@Test
	public void testSimple() {
		ClassDescriptor descriptor = createClassDescriptor(TestModel.class);
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
	public void testTsSimple() {
		ClassDescriptor descriptor = createClassDescriptor(TestTsModel.class);
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals("TestTsModel", generate.getName());
		Assert.assertEquals(TestTsModel.class.getName(), generate.getFullSlingModelName());
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("model", field.getName());
		Assert.assertEquals("X", field.getTypes()[0]);
		Assert.assertEquals(1, generate.getImports().size());
		ImportModel importModel = generate.getImports().iterator().next();
		Assert.assertEquals("X", importModel.getName());
		Assert.assertEquals("ts/Element", importModel.getPath());
	}

	@Test
	public void testComplex() {
		ClassDescriptor descriptor = createClassDescriptor(TestComplexModel.class);
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
		ClassDescriptor descriptor = createClassDescriptor(TestArrayModel.class);
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("models", field.getName());
		Assert.assertEquals("TestModel[]", field.getTypes()[0]);
		Assert.assertEquals(1, generate.getImports().size());
		Assert.assertEquals("TestModel", generate.getImports().iterator().next().getName());
	}

	@Test
	public void testAnyArray() {
		ClassDescriptor descriptor = createClassDescriptor(TestAnyArrayModel.class);
		InterfaceModel generate = TypeScriptGenerator.builder().build().generateModel(descriptor);
		Assert.assertEquals(1, generate.getFields().size());
		FieldModel field = generate.getFields().iterator().next();
		Assert.assertEquals("models", field.getName());
		Assert.assertEquals("any[]", field.getTypes()[0]);
		Assert.assertEquals(0, generate.getImports().size());
	}

	@Test
	public void testTsList() {
		ClassDescriptor descriptor = createClassDescriptor(TestTsListModel.class, "../src");
		TypeScriptGenerator typeScriptGenerator = TypeScriptGenerator.builder().build();
		InterfaceModel generate = typeScriptGenerator.generateModel(descriptor);
		assertThat(generate.getFields()).hasSize(1);
		FieldModel field = generate.getFields().iterator().next();
		assertThat(field.getName()).isEqualTo("models");
		assertThat(field.getTypes()).containsOnly("Element[]");
		assertThat(generate.getImports()).hasSize(1);
		ImportModel generatedImport = generate.getImports().iterator().next();
		assertThat(generatedImport.getName()).isEqualTo("Element");
		assertThat(generatedImport.getPath()).isEqualTo("../src/ts/Element");
	}

	@Test
	public void testListAnnotationOnGetter() {
		ClassDescriptor descriptor = createClassDescriptor(TestGetterListModel.class);
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
		ClassDescriptor descriptor = createClassDescriptor(TestMapModel.class);
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

		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(Sub1.class, ctx, null,
				new PathMapper(Sub1.class.getName()), new TsPathMapper(Sub1.class.getName(), null, null));
		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);

		Assert.assertEquals("BaseBaseModel", descriptor.getSuperClass().getType());
		Assert.assertEquals(1, model.getImports().size());
		Assert.assertEquals("BaseBaseModel", model.getSuperClasses());
		Assert.assertNotNull(descriptor.getDiscriminator());
		Assert.assertNotNull(model.getDiscriminator());
		Assert.assertEquals("kind", model.getDiscriminator().getField());
		Assert.assertEquals("sub1", model.getDiscriminator().getValue());
		Assert.assertNotNull(model.getSuperClasses());
		Assert.assertEquals(1, model.getFields().size());
	}

	@Test
	public void testDeepSubclassing() {
		ScanContext ctx = new ScanContext();
		DiscriminatorPreprocessor.findDiscriminators(BaseModel.class, new PathMapper(BaseModel.class.getName()), ctx);

		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(Sub2.class, ctx, null,
				new PathMapper(Sub2.class.getName()), new TsPathMapper(Sub2.class.getName(), null, null));
		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);

		Assert.assertEquals("BaseBaseModel", descriptor.getSuperClass().getType());
		Assert.assertEquals(1, model.getImports().size());
		Assert.assertEquals("BaseBaseModel", model.getSuperClasses());
		Assert.assertNull(descriptor.getDiscriminator());
		Assert.assertNull(model.getDiscriminator());

		ClassDescriptor descriptor2 = GeneratorFromClass.createClassDescriptor(Sub3.class, ctx, null,
				new PathMapper(Sub3.class.getName()), new TsPathMapper(Sub3.class.getName(), null, null));
		InterfaceModel model2 = TypeScriptGenerator.builder().build().generateModel(descriptor2);

		Assert.assertNotNull(descriptor2.getDiscriminator());
		Assert.assertNotNull(model2.getDiscriminator());
		Assert.assertEquals("Sub2", descriptor2.getSuperClass().getType());

		Assert.assertEquals(1, model2.getImports().size());
		Assert.assertEquals("Sub2", model2.getSuperClasses());
		Assert.assertEquals("kind", model2.getDiscriminator().getField());
		Assert.assertEquals("sub3", model2.getDiscriminator().getValue());
		Assert.assertNotNull(model2.getSuperClasses());
		Assert.assertEquals(1, model2.getFields().size());
	}

	@Test
	public void testUnionType() {
		ScanContext ctx = new ScanContext();
		DiscriminatorPreprocessor.findDiscriminators(BaseModel.class, new PathMapper(BaseModel.class.getName()), ctx);
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(BaseModel.class, ctx, null,
				new PathMapper(BaseModel.class.getName()), new TsPathMapper(BaseModel.class.getName(), null, null));
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

	@Test
	public void testComponentExporter() {
		ClassDescriptor descriptor = createClassDescriptor(ComponentModel.class);

		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);
		assertThat(model).isNotNull();
		assertThat(model.getFields()).containsOnly(new FieldModel("stringField", "string"));
	}

	@Test
	public void testContainerExporterTsElement() {
		ClassDescriptor descriptor = createClassDescriptor(ContainerModelTsElement.class, "../src");

		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);
		assertThat(model).isNotNull();

		assertThat(model.getFields()).containsOnly(
				new FieldModel("stringField", "string"),
				new FieldModel("booleanField", "boolean")
		);

		assertThat(model.getImports()).hasSize(1);
		ImportModel generatedImport = model.getImports().iterator().next();
		assertThat(generatedImport.getName()).isEqualTo("ContainerExporter");
		assertThat(generatedImport.getPath()).isEqualTo("../src/infrastructure/ContainerExporter");

		assertThat(model.getSuperClasses()).isEqualTo("ContainerExporter");
	}

	@Test
	public void testContainerExporterNoTsElement() {
		Class<ContainerModelNoTsElement> clazz = ContainerModelNoTsElement.class;
		PathMapper mapper = new PathMapper(clazz.getName());
		TsPathMapper tsPathMapper = new TsPathMapper(clazz.getName(), clazz.getPackage().getName(), "../src");

		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(clazz, new ScanContext(),
				null, mapper, tsPathMapper);

		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);
		assertThat(model).isNotNull();

		assertThat(model.getImports()).hasSize(1);
		ImportModel generatedImport = model.getImports().iterator().next();
		assertThat(generatedImport.getName()).isEqualTo("ContainerExporter");
		assertThat(generatedImport.getPath()).isEqualTo("../../../../../adobe/cq/export/json/ContainerExporter");

		assertThat(model.getSuperClasses()).isEqualTo("ContainerExporter");
	}

	@Test
	public void testContainerExporterTsDefault() {
		Class<ContainerModelNoTsElement> clazz = ContainerModelNoTsElement.class;
		ClassDescriptor descriptor = GeneratorFromClass.createClassDescriptor(
				clazz,
				new ScanContext(),
				Collections.singletonList(new TsElementDefault(
						ContainerExporter.class.getCanonicalName(),
						null,
						"infrastructure/ContainerExporter")
				),
				new PathMapper(clazz.getName()),
				new TsPathMapper(clazz.getName(), clazz.getPackage().getName(), "../src"));

		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);
		assertThat(model).isNotNull();

		assertThat(model.getImports()).hasSize(1);
		ImportModel generatedImport = model.getImports().iterator().next();
		assertThat(generatedImport.getName()).isEqualTo("ContainerExporter");
		assertThat(generatedImport.getPath()).isEqualTo("../src/infrastructure/ContainerExporter");

		assertThat(model.getSuperClasses()).isEqualTo("ContainerExporter");
	}

	@Test
	public void testSubContainerExporterTsElement() {
		ClassDescriptor descriptor = createClassDescriptor(SubContainerModelTsElement.class, "../src");

		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);
		assertThat(model).isNotNull();
		assertThat(model.getFields()).containsOnly(
				new FieldModel("stringField", "string")
		);

		assertThat(model.getImports()).hasSize(1);
		ImportModel generatedImport = model.getImports().iterator().next();
		assertThat(generatedImport.getName()).isEqualTo("SubContainerExporter");
		assertThat(generatedImport.getPath()).isEqualTo("../src/spa/SubContainerExporter");

		assertThat(model.getSuperClasses()).isEqualTo("SubContainerExporter");
	}

	@Test
	public void testSubContainerExporterNoTsElement() {
		ClassDescriptor descriptor = createClassDescriptor(SubContainerModelNoTsElement.class, "../src");

		InterfaceModel model = TypeScriptGenerator.builder().build().generateModel(descriptor);
		assertThat(model).isNotNull();
		assertThat(model.getFields()).containsOnly(
				new FieldModel("stringField", "string")
		);

		assertThat(model.getImports()).hasSize(1);
		ImportModel generatedImport = model.getImports().iterator().next();
		assertThat(generatedImport.getName()).isEqualTo("SubContainerExporter");
		assertThat(generatedImport.getPath()).isEqualTo("./SubContainerExporter");

		assertThat(model.getSuperClasses()).isEqualTo("SubContainerExporter");
	}
}
