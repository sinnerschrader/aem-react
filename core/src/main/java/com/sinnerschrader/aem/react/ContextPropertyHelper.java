package com.sinnerschrader.aem.react;

import org.apache.sling.commons.osgi.PropertiesUtil;

import java.util.Dictionary;

class ContextPropertyHelper {

    private final Dictionary contextProperties;

    ContextPropertyHelper(Dictionary contextProperties) {
        this.contextProperties = contextProperties;
    }

    String toString(String propName, String defaultValue) {
        return PropertiesUtil.toString(contextProperties.get(propName), defaultValue);
    }

    String[] toStringArray(String propName, String[] defaultArray) {
        return PropertiesUtil.toStringArray(contextProperties.get(propName), defaultArray);
    }

    int toInteger(String propName, int defaultValue) {
        return PropertiesUtil.toInteger(contextProperties.get(propName), defaultValue);
    }

    boolean toBoolean(String propName, boolean defaultValue) {
        return PropertiesUtil.toBoolean(contextProperties.get(propName), defaultValue);
    }

}
