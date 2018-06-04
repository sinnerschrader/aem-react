package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.sinnerschrader.aem.reactapi.typescript.NotNull;

public class TestNotNull {

    private String value;

    @NotNull
    public String getValue() {
        return value;
    }

    private boolean done;

    public boolean isDone() {
        return done;
    }

}
