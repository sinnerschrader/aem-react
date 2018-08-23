package com.sinnerschrader.aem.react;

public class JsExecutionStack {

	public interface JsExecutionProcessor<T> {
		public T execute(int level) throws Exception;
	}


	public static <T> T execute(JsExecutionProcessor<T> process) throws Exception
	{
		int level = JsExecutionLevel.incAndGet();
		try {
			return process.execute(level);
		}finally {
			JsExecutionLevel.dec();
		}
	}

}
