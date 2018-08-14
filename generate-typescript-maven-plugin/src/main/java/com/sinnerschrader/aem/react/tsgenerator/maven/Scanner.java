package com.sinnerschrader.aem.react.tsgenerator.maven;

import java.util.List;

import com.sinnerschrader.aem.react.tsgenerator.generator.TsPathMapper;
import org.apache.maven.plugin.logging.Log;

import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ScanContext;
import com.sinnerschrader.aem.react.tsgenerator.fromclass.DiscriminatorPreprocessor;
import com.sinnerschrader.aem.react.tsgenerator.fromclass.GeneratorFromClass;
import com.sinnerschrader.aem.react.tsgenerator.generator.PathMapper;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import lombok.Builder;

@Builder
public class Scanner {

	private Log log;

	private String basePackage;
	private String baseTypeScriptPath;
	private List<TsElementDefault> tsElementDefaults;
	private Class<?> annotationClass;

	public void scan(Processor processor, EnumProcessor enumProcessor) {
		FastClasspathScanner scanner = new FastClasspathScanner(basePackage);
		ScanResult scanResult = scanner.scan();
		ScanContext ctx = new ScanContext();
		List<String> aClasses = scanResult.getNamesOfClassesWithAnnotation(annotationClass);
		for (String clazzName : aClasses) {
			try {
				Class<?> clazz = Class.forName(clazzName);

				DiscriminatorPreprocessor.findDiscriminators(clazz, new PathMapper(clazz.getName()), ctx);

			} catch (ClassNotFoundException e) {
				log.error(String.format("class not found %s.", e.getMessage()));
			}

		}
		for (String clazzName : aClasses) {
			try {
				Class<?> clazz = Class.forName(clazzName);
				if (Enum.class.isAssignableFrom(clazz)) {
					EnumDescriptor cd = GeneratorFromClass.createEnumDescriptor((Class<Enum>) clazz);
					enumProcessor.apply(cd);
				} else {
					PathMapper mapper = new PathMapper(clazz.getName());
					TsPathMapper tsPathMapper = new TsPathMapper(clazz.getName(), basePackage, baseTypeScriptPath);
					ClassDescriptor cd = GeneratorFromClass.createClassDescriptor(clazz, ctx, tsElementDefaults, mapper, tsPathMapper);
					processor.apply(cd);
				}
			} catch (ClassNotFoundException e) {
				log.error(String.format("class not found %s.", e.getMessage()));
			}
		}
	}
}
