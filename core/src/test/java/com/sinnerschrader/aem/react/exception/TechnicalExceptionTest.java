package com.sinnerschrader.aem.react.exception;

import org.junit.Test;

public class TechnicalExceptionTest {

	@Test
	public void createString() {
		new TechnicalException("test");
	}

	@Test
	public void createStringAndError() {
		new TechnicalException("test", new RuntimeException());
	}

}
