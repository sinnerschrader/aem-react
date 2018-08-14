package com.sinnerschrader.aem.react.tsgenerator.fromclass;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassBeanTest {

	@Test
	public void testJavaClass() {
		ClassBean classBean = new ClassBean("nn.ff.Hallo");

		assertThat(classBean.getName()).isEqualTo("nn.ff.Hallo");
		assertThat(classBean.getSimpleName()).isEqualTo("Hallo");
		assertThat(classBean.isExtern()).isTrue();
	}

	@Test
	public void testTsClass() {
		ClassBean classBean = new ClassBean("nn/ff/Hallo");

		assertThat(classBean.getName()).isEqualTo("nn/ff/Hallo");
		assertThat(classBean.getSimpleName()).isEqualTo("Hallo");
		assertThat(classBean.isExtern()).isTrue();
	}

}
