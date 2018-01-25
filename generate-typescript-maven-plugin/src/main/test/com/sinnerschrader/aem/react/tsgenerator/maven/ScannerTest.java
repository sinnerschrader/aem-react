package com.sinnerschrader.aem.react.tsgenerator.maven;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.UnionType;
import com.sinnerschrader.aem.reactapi.typescript.ExportTs;

public class ScannerTest {
	@Test
	public void test() {
		Scanner scanner = Scanner.builder()//
				.basePackage("com.sinnerschrader.aem.react.tsgenerator")//
				.annotationClass(ExportTs.class)//
				.build();

		final List<ClassDescriptor> cds = new ArrayList<>();
		final List<EnumDescriptor> eds = new ArrayList<>();
		final List<UnionType> uts = new ArrayList<>();
		scanner.scan((ClassDescriptor cd) -> {
			cds.add(cd);
		}, (EnumDescriptor ed) -> {
			eds.add(ed);
		});

		Assert.assertEquals(9, cds.size());
		Assert.assertEquals(0, uts.size());
		Assert.assertEquals(0, eds.size());
	}
}
