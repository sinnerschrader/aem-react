package com.sinnerschrader.aem.react.tsgenerator.maven;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.generator.TypeScriptGenerator;
import com.sinnerschrader.aem.react.tsgenerator.generator.model.InterfaceModel;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateTsMojo extends AbstractMojo {

	@Parameter
	private File targetDirectory;

	@Parameter
	private File templateFolder;

	@Parameter
	private String annotationClassName;

	@Parameter
	private String basePackage;

	@Parameter
	private String typeScriptRelativeBasePath;

	@Parameter
	private TsElementDefault[] tsElementDefaults;

	@Parameter(defaultValue = "utf-8")
	private String encoding;

	@Override
	public void execute() throws MojoExecutionException {
		prepare(targetDirectory);

		Class<?> annotationClass;
		try {
			annotationClass = Class.forName(annotationClassName);
		} catch (ClassNotFoundException e1) {
			throw new MojoExecutionException("cannot find annotation class" + annotationClassName, e1);
		}

		Scanner scanner = Scanner.builder()//
				.annotationClass(annotationClass)//
				.basePackage(basePackage)//
				.baseTypeScriptPath(this.typeScriptRelativeBasePath)
				.tsElementDefaults(asList(this.tsElementDefaults))
				.log(getLog())//
				.build();

		TypeScriptGenerator typeScriptGenerator = TypeScriptGenerator.builder()//
				.log(getLog())//
				.templateFolder(templateFolder)//
				.build();

		scanner.scan((ClassDescriptor cd) -> {
			InterfaceModel m = typeScriptGenerator.generateModel(cd);
			String s = typeScriptGenerator.generate(m);
			File targetFile = createTargetFile(cd.getFullJavaClassName());
			writeStringToFile(s, targetFile);
		}, (EnumDescriptor ed) -> {
			String s = typeScriptGenerator.generateEnum(ed);
			File targetFile = createTargetFile(ed.getFullJavaClassName());
			writeStringToFile(s, targetFile);
		});
	}

	private static <T> List<T> asList(T[] values) {
		if (values == null || values.length == 0) {
			return Collections.emptyList();
		}
		return Arrays.asList(values);
	}

	private void prepare(File f) {
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Could not create directory " + f);
			}
		}
	}

	private File createTargetFile(String fullJavaClassName) {
		String relPath = fullJavaClassName.substring(basePackage.length() + 1).replace('.', '/');
		return new File(targetDirectory, relPath + ".ts").getAbsoluteFile();
	}

	private void writeStringToFile(String s, File target) {
		getLog().info("Generating " + target.getAbsolutePath());
		prepare(target.getParentFile());
		try (FileWriter fw = new FileWriter(target)) {
			IOUtils.copy(new StringReader(s), fw);
		} catch (IOException e) {
			throw new RuntimeException("error", e);
		}
	}

}
