package com.sinnerschrader.aem.react;

public class JsExecutionLevel{

		private static final ThreadLocal<Integer> level = new ThreadLocal<Integer>() {
			@Override
			protected Integer initialValue() {
				return 0;
			}
		};

		public static int get() {
			return level.get();
		}

		public static int incAndGet() {
			int currentLevel = level.get();
			currentLevel++;
			level.set(currentLevel);
			return currentLevel;
		}

		public static void dec() {
			int currentLevel = level.get();
			currentLevel--;
			level.set(currentLevel);
		}


}
