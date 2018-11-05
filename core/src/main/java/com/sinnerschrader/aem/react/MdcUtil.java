package com.sinnerschrader.aem.react;

import org.slf4j.MDC;

public final class MdcUtil {

    public static void addToMdc(long startTs, String name) {
        addToMdc(startTs, name, null);
    }

    public static void addToMdc(long startTs, String name, Object detail) {
        if (!"true".equals(MDC.get("enable_react_mdc"))) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startTs;
        String mdcValue = MDC.get("react");
        StringBuilder sb = new StringBuilder();
        if (mdcValue != null) {
            sb.append(mdcValue).append(',');
        }
        sb.append(name);
        if (detail != null) {
            sb.append('/').append(detail);
        }
        sb.append('=').append(elapsed);

        MDC.put("react", sb.toString());
    }
}
