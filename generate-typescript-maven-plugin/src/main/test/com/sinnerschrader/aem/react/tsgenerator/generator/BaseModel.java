package com.sinnerschrader.aem.react.tsgenerator.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.sinnerschrader.aem.reactapi.typescript.ExportTs;

import lombok.Data;

@ExportTs
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "kind")
@JsonSubTypes({ @Type(value = Sub1.class, name = "sub1"), @Type(value = Sub2.class, name = "sub2") })
@Data
public abstract class BaseModel {
	private String value;

}
