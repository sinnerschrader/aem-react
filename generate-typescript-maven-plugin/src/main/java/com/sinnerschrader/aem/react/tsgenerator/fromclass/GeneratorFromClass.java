package com.sinnerschrader.aem.react.tsgenerator.fromclass;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang3.EnumUtils;

import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor.ClassDescriptorBuilder;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.Discriminator;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ScanContext;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.TypeDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.UnionType;
import com.sinnerschrader.aem.react.tsgenerator.generator.PathMapper;
import com.sinnerschrader.aem.reactapi.typescript.Element;

public class GeneratorFromClass {
	public static final Set<String> BLACKLIST = Collections.unmodifiableSet(new HashSet<String>() {
		{
			add("class");
		}
	});

	public static TypeDescriptor convertType(Class<?> type, Element element, PathMapper mapper) {
		TypeDescriptor.TypeDescriptorBuilder td = TypeDescriptor.builder();
		StringBuffer typeDeclaration = new StringBuffer();
		final Class<?> propertyType;

		if (type.isArray()) {
			td.array(true);
			propertyType = ClassUtils.primitiveToWrapper(type.getComponentType());
		} else if (Collection.class.isAssignableFrom(type)) {
			td.array(true);
			propertyType = element == null ? Object.class : element.value();
		} else if (Map.class.isAssignableFrom(type)) {
			td.map(true);
			propertyType = element == null ? Object.class : element.value();
		} else {
			propertyType = ClassUtils.primitiveToWrapper(type);
			td.array(false);
		}

		if (String.class.isAssignableFrom(propertyType)) {
			typeDeclaration.append(TypeDescriptor.STRING);
		} else if (Number.class.isAssignableFrom(propertyType)) {
			typeDeclaration.append(TypeDescriptor.NUMBER);
		} else if (Boolean.class.isAssignableFrom(propertyType)) {
			typeDeclaration.append(TypeDescriptor.BOOL);
		} else {
			typeDeclaration.append(propertyType.getSimpleName());
			String path = mapper.apply(propertyType.getName());
			td.path(path)//
					.extern(true);
		}

		td.type(typeDeclaration.toString());

		return td.build();
	}

	private static String getName(boolean unionType, Class<?> clazz) {
		return !unionType ? clazz.getSimpleName() : "Base" + clazz.getSimpleName();
	}

	public static ClassDescriptor createClassDescriptor(Class<?> clazz, ScanContext ctx, PathMapper mapper) {
		try {
			ClassDescriptorBuilder builder = ClassDescriptor.builder();
			UnionType unionType = ctx.unionTypes.get(clazz);

			BeanInfo info = Introspector.getBeanInfo(clazz);
			builder.name(getName(unionType != null, info.getBeanDescriptor().getBeanClass()));
			builder.fullJavaClassName(info.getBeanDescriptor().getBeanClass().getName());
			Discriminator discriminator = ctx.discriminators.get(clazz);

			Class<?> superClass = info.getBeanDescriptor().getBeanClass().getSuperclass();
			if (!superClass.equals(Object.class)) {
				String superClassName = getName(discriminator != null, superClass);
				builder.superClass(TypeDescriptor.builder()//
						.type(superClassName)//
						.extern(false)//
						.path(mapper.apply(superClass.getName()))//
						.build());

			}

			builder.discriminator(discriminator);
			builder.unionType(unionType);

			ClassDescriptor cd = builder.build();

			for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
				if (!BLACKLIST.contains(pd.getName()) && (pd.getReadMethod() != null || pd.getWriteMethod() != null)) {
					Element fieldAnnotation = null;
					boolean inherited = false;
					try {
						Field declaredField = clazz.getDeclaredField(pd.getName());
						inherited = declaredField.getDeclaringClass() != clazz;
						fieldAnnotation = declaredField.getAnnotation(Element.class);

					} catch (NoSuchFieldException | SecurityException e) {
						//
					}
					if (pd.getReadMethod() != null) {
						inherited = pd.getReadMethod().getDeclaringClass() != clazz;
					}
					if (inherited) {
						continue;
					}
					Element getterAnnotation = pd.getReadMethod() != null
							? pd.getReadMethod().getAnnotation(Element.class)
							: null;
					Element element = Optional//
							.ofNullable(getterAnnotation)//
							.orElse(fieldAnnotation);

					com.sinnerschrader.aem.react.tsgenerator.descriptor.PropertyDescriptor pdd = com.sinnerschrader.aem.react.tsgenerator.descriptor.PropertyDescriptor
							.builder()//
							.name(pd.getName())//
							.type(convertType(pd.getPropertyType(), element, mapper))//
							.build();

					cd.getProperties().put(pdd.getName(), pdd);
				}

			}

			return cd;
		} catch (IntrospectionException e) {
			return null;
		}
	}

	public static ClassDescriptor createClassDescriptor(String clazzName, ScanContext ctx, PathMapper mapper) {
		try {
			return createClassDescriptor(Class.forName(clazzName), ctx, mapper);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("error", e);
		}
	}

	public static <E extends Enum<E>> EnumDescriptor createEnumDescriptor(Class<E> enumClass) {
		Map<String, E> map = EnumUtils.getEnumMap(enumClass);

		return EnumDescriptor.builder()//
				.name(enumClass.getSimpleName())//
				.fullJavaClassName(enumClass.getName())//
				.values(map.values().stream().map((E e) -> e.name()).sorted().collect(Collectors.toList()))//
				.build();
	}

}
