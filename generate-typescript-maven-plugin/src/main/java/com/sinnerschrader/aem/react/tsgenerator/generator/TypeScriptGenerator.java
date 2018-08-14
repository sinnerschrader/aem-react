package com.sinnerschrader.aem.react.tsgenerator.generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.sinnerschrader.aem.react.tsgenerator.descriptor.ExtendedInterface;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.UnionType;
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

	public InterfaceModel generateModel(final ClassDescriptor descriptor) {
		SortedSet<ImportModel> imports = new TreeSet<>();
		List<String> superClasses = new ArrayList<>();
		if (descriptor.getSuperClass() != null) {
			TypeDescriptor sct = descriptor.getSuperClass();
			superClasses.add(sct.getType());
			imports.add(new ImportModel(sct.getType(), sct.getPath()));
		}

		for (ExtendedInterface ei : descriptor.getExtendedInterfaces().values()) {
			ImportModel importModel = new ImportModel(ei.getName(), ei.getPath());
			imports.add(importModel);
			superClasses.add(importModel.getName());
		}

		SortedSet<FieldModel> fields = new TreeSet<>();
		for (PropertyDescriptor prop : descriptor.getProperties().values()) {
			TypeDescriptor propType = prop.getType();
			if (propType.isExtern()) {
				imports.add(new ImportModel(propType.getType(), propType.getPath()));
			}

			final String fullType = propType.isArray()
					? propType.getType() + "[]"
					: propType.isMap()
						? "{[key: string]: " + propType.getType() + "}"
						: propType.getType();
			fields.add(new FieldModel(prop.getName(), fullType));
		}

		UnionModel unionModel = null;
		UnionType unionType = descriptor.getUnionType();
		if (unionType != null) {
			PathMapper pathMapper = new PathMapper(descriptor.getFullJavaClassName());
			for (Discriminator d : unionType.getDiscriminators()) {
				String mappedPath = pathMapper.apply(d.getType().getName());
				imports.add(new ImportModel(d.getType().getSimpleName(), mappedPath));
			}
			unionModel = UnionModel.builder()//
					.name(unionType.getDescriptor().getType())//
					.field(unionType.getField())//
					.types(unionType.getDiscriminators().stream()//
							.map((Discriminator d) -> d.getType().getSimpleName())//
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

		String superclasses = String.join(", ", superClasses);

		InterfaceModel model = InterfaceModel.builder()//
				.name(descriptor.getName())//
				.superClasses(superclasses)//
				.unionModel(unionModel)//
				.fullSlingModelName(descriptor.getFullJavaClassName())//
				.fields(fields)//
				.discriminator(discriminator)//
				.imports(imports)//
				.build();

		return model;
	}

	public String generate(InterfaceModel model) {
		StringBuilder buffer = new StringBuilder();
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

}
