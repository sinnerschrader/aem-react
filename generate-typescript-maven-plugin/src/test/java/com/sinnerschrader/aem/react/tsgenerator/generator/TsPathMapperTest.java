package com.sinnerschrader.aem.react.tsgenerator.generator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TsPathMapperTest {

	@Test
	public void ensureTrailingSlash() {
		assertThat(TsPathMapper.ensureTrailingSlash(null)).isNull();
		assertThat(TsPathMapper.ensureTrailingSlash("")).isEqualTo("");
		assertThat(TsPathMapper.ensureTrailingSlash("foo/")).isEqualTo("foo/");
		assertThat(TsPathMapper.ensureTrailingSlash("foo")).isEqualTo("foo/");
	}

	@Test
	public void countPackageDepth() {
		assertThat(TsPathMapper.countPackageDepth("Foo")).isEqualTo(0);
		assertThat(TsPathMapper.countPackageDepth("foo.Bar")).isEqualTo(1);
		assertThat(TsPathMapper.countPackageDepth("foo.bar.Baz")).isEqualTo(2);
	}

	@Test
	public void apply() {
		TsPathMapper tsPathMapper = new TsPathMapper("foo.bar.baz.core.models.page.PageModel", "foo.bar.baz", "../src");
		assertThat(tsPathMapper.apply("modules/spa/Compound")).isEqualTo("../../../../src/modules/spa/Compound");
		assertThat(tsPathMapper.apply("/modules/spa/Compound")).isEqualTo("modules/spa/Compound");

		assertThat(tsPathMapper.apply("any")).isEqualTo("any");
	}
}
