package com.sinnerschrader.aem.react.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.sinnerschrader.aem.react.exception.TechnicalException;

public class PathUtils {

	public static String decode(String path) {
		try {
			return URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new TechnicalException("cannot decode path +path, e");
		}
	}

}
