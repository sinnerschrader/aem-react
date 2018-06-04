package com.sinnerschrader.aem.react.tsgenerator.generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.inject.internal.util.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateSource;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.Discriminator;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.PropertyDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.TypeDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.DiscriminatorModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.FieldModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.ImportModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.InterfaceModel;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.UnionModel;

import lombok.Builder;

@Builder
public class TypeScriptGenerator {

	private File templateFolder;
	private Log log;

	public String generateEnum(EnumDescriptor descriptor) {
		Handlebars handleBars = new Handlebars();
		try {
			handleBars.registerHelper("join", StringHelpers.join);

			Template template = handleBars.compile(getTemplate("enum"));
			return template.apply(descriptor);
		} catch (IOException e) {
			log.error("cannot generate enum", e);
			return "error";
		}
	}

	public InterfaceModel generateModel(ClassDescriptor descriptor) {

		PathMapper pathMapper = new PathMapper(descriptor.getFullJavaClassName());
		SortedSet<ImportModel> imports = new TreeSet<>();
		String superclass = null;
		if (descriptor.getSuperClass() != null) {
			TypeDescriptor sct = descriptor.getSuperClass();
			superclass = sct.getType();
			imports.add(ImportModel.builder()//
					.name(sct.getType())//
					.path(sct.getPath())//
					.build());
		}

		SortedSet<FieldModel> fields = new TreeSet<>();
		for (PropertyDescriptor prop : descriptor.getProperties().values()) {
			if (prop.getType().isExtern()) {
				imports.add(ImportModel.builder()//
						.name(prop.getType().getType())//
						.path(prop.getType().getPath())//
						.build());
			}

			fields.add(FieldModel.builder()//
					.name(prop.getName())//
					.types(getTypes(prop))//
					.build());
		}

		UnionModel unionModel = null;
		if (descriptor.getUnionType() != null) {
			descriptor.getUnionType().getDiscriminators().stream()//
					.forEach((Discriminator d) -> {
						imports.add(ImportModel.builder()//
								.name(d.getType().getSimpleName())//
								.path(pathMapper.apply(d.getType().getName()))//
								.build());
					});
			unionModel = UnionModel.builder()//
					.name(descriptor.getUnionType().getDescriptor().getType())//
					.field(descriptor.getUnionType().getField())//
					.types(descriptor.getUnionType().getDiscriminators().stream()//
							.map((Discriminator d) -> {
								return d.getType().getSimpleName();
							})//
							.collect(Collectors.toList()))//
					.build();
		}
		DiscriminatorModel discriminator = null;
		if (descriptor.getDiscriminator() != null) {
			discriminator = DiscriminatorModel.builder()//
					.field(descriptor.getDiscriminator().getField())//
					.value(descriptor.getDiscriminator().getValue())//
					.build();
		}
		InterfaceModel model = InterfaceModel.builder()//
				.name(descriptor.getName())//
				.superclass(superclass)//
				.unionModel(unionModel)//
				.fullSlingModelName(descriptor.getFullJavaClassName())//
				.fields(fields)//
				.discriminator(discriminator)//
				.imports(imports)//
				.build();

		return model;
	}

	public String generate(InterfaceModel model) {
		StringBuffer buffer = new StringBuffer();
		Handlebars handleBars = new Handlebars();
		try {
			handleBars.registerHelper("join", StringHelpers.join);
			Template template = handleBars.compile(getTemplate("model"));
			String apply = template.apply(model);
			buffer.append(apply);
		} catch (IOException e) {
			log.error("cannot generate ts ", e);
		}

		return buffer.toString();
	}

	private TemplateSource getTemplate(String name) throws IOException {
		File file;
		if (templateFolder == null) {
			file = new File(getClass().getResource("/com/sinnerschrader/aem/react/tsgenerator" + "/" + name + ".hbs")
					.getFile());
		} else {
			file = new File(templateFolder, name + ".hbs");
		}
		String content = FileUtils.readFileToString(file);
		return new StringTemplateSource(file.getAbsolutePath(), content);

	}

	private String[] getTypes(PropertyDescriptor prop) {
		final String fullType = prop.getType().isArray() ? prop.getType().getType() + "[]"
				: prop.getType().isMap() ? "{[key: string]: " + prop.getType().getType() + "}"
				: prop.getType().getType();

		ImmutableList.Builder<String> builder = ImmutableList.builder();
		builder.add(fullType);
		if(!prop.isNotNullable()) {
			builder.add(TypeDescriptor.NULL);
		}
		return builder.build().toArray(new String[]{});
	}

}
