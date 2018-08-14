package com.sinnerschrader.aem.react.tsgenerator.fromclass;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.Discriminator;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ScanContext;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.TypeDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.UnionType;
import com.sinnerschrader.aem.react.tsgenerator.generator.PathMapper;

public class DiscriminatorPreprocessor {

	public static void findDiscriminators(Class<?> type, PathMapper mapper, ScanContext ctx) {
		JsonSubTypes subTypes = type.getAnnotation(JsonSubTypes.class);
		if (subTypes == null) {
			return;
		}
		JsonTypeInfo info = type.getAnnotation(JsonTypeInfo.class);
		if (info == null) {
			return;
		}

		List<Discriminator> discriminators = new ArrayList<>();
		for (Type subtype : subTypes.value()) {
			Discriminator discriminator = Discriminator.builder()//
					.field(info.property())//
					.value(subtype.name())//
					.type(subtype.value())//
					.build();
			ctx.discriminators.put(subtype.value(), discriminator);
			discriminators.add(discriminator);
		}
		UnionType unionType = UnionType.builder()//
				.descriptor(TypeDescriptor.builder()//
						.path(mapper.apply(type.getName()))//
						.type(type.getSimpleName())//
						.build())//
				.field(info.property())//
				.discriminators(discriminators)//
				.build();
		ctx.unionTypes.put(type, unionType);
	}

}
