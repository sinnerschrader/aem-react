package com.sinnerschrader.aem.react.tsgenerator.fromclass;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sinnerschrader.aem.react.tsgenerator.descriptor.ExtendedInterface;
import com.sinnerschrader.aem.react.tsgenerator.maven.TsElementDefault;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.EnumUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ClassDescriptor.ClassDescriptorBuilder;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.Discriminator;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.EnumDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.ScanContext;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.TypeDescriptor;
import com.sinnerschrader.aem.react.tsgenerator.descriptor.UnionType;
import com.sinnerschrader.aem.react.tsgenerator.generator.PathMapper;
import com.sinnerschrader.aem.react.tsgenerator.generator.TsPathMapper;
import com.sinnerschrader.aem.reactapi.typescript.Element;
import com.sinnerschrader.aem.reactapi.typescript.TsElement;
import org.codehaus.plexus.util.StringUtils;

public class GeneratorFromClass {

	private static final Set<String> BLACKLIST = Collections.singleton("class");
	private static final Map<String, Set<String>> BLACKLIST_BY_CLASSNAME;
	static {
		Map<String, Set<String>> map = new HashMap<>();

		map.put("com.adobe.cq.export.json.ComponentExporter", Collections.singleton("exportedType"));
		map.put("com.adobe.cq.export.json.ContainerExporter", new HashSet<>(Arrays.asList(
				"exportedType",
				"exportedItems",
				"exportedItemsOrder"))
		);

		BLACKLIST_BY_CLASSNAME = map;
	}


	public static TypeDescriptor convertType(Class<?> type, ClassBean element, PathMapper mapper) {
		TypeDescriptor.TypeDescriptorBuilder td = TypeDescriptor.builder();
		StringBuilder typeDeclaration = new StringBuilder();
		final ClassBean propertyType;

		if (type.isArray()) {
			td.array(true);
			if (element != null) {
				propertyType = element;
			} else {
				propertyType = new ClassBean(ClassUtils.primitiveToWrapper(type.getComponentType()));
			}
		} else if (Collection.class.isAssignableFrom(type)) {
			td.array(true);
			propertyType = element;
		} else if (Map.class.isAssignableFrom(type)) {
			td.map(true);
			propertyType = element;
		} else if (element != null) {
			propertyType = element;
			td.array(false);
		} else {
			propertyType = new ClassBean(ClassUtils.primitiveToWrapper(type));
			td.array(false);
		}

		if (propertyType.isString()) {
			typeDeclaration.append(TypeDescriptor.STRING);
		} else if (propertyType.isNumber()) {
			typeDeclaration.append(TypeDescriptor.NUMBER);
		} else if (propertyType.isBoolean()) {
			typeDeclaration.append(TypeDescriptor.BOOL);
		} else {
			typeDeclaration.append(propertyType.getSimpleName());
			String path = propertyType.getPathMapper() != null
					? propertyType.getPathMapper().apply(propertyType.getName())
					: mapper.apply(propertyType.getName());
			td.path(path)//
					.extern(propertyType.isExtern());
		}

		td.type(typeDeclaration.toString());

		return td.build();
	}

	private static String getName(boolean unionType, Class<?> clazz) {
		return unionType ? "Base" + clazz.getSimpleName() : clazz.getSimpleName();
	}

	public static ClassDescriptor createClassDescriptor(Class<?> clazz, ScanContext ctx,
			List<TsElementDefault> tsElementDefaults, PathMapper mapper, TsPathMapper tsPathMapper) {
		try {
			ClassDescriptorBuilder builder = ClassDescriptor.builder();

			UnionType unionType = ctx.unionTypes.get(clazz);
			builder.unionType(unionType);

			BeanInfo info = Introspector.getBeanInfo(clazz);
			BeanDescriptor beanDescriptor = info.getBeanDescriptor();
			builder.name(getName(unionType != null, beanDescriptor.getBeanClass()));
			builder.fullJavaClassName(beanDescriptor.getBeanClass().getName());

			Discriminator discriminator = ctx.discriminators.get(clazz);
			builder.discriminator(discriminator);

			Class<?> superClass = beanDescriptor.getBeanClass().getSuperclass();
			if (!superClass.equals(Object.class)) {
				String superClassName = getName(ctx.unionTypes.get(superClass) != null, superClass);
				TypeDescriptor typeDescriptor = TypeDescriptor.builder()//
						.type(superClassName)//
						.extern(false)//
						.path(mapper.apply(superClass.getName()))//
						.build();
				builder.superClass(typeDescriptor);
			}

			final Map<String, ExtendedInterface> tsInterfaces = collectTsInterfaces(clazz, tsElementDefaults, mapper, tsPathMapper);
			final Set<String> blacklist = createClassBlacklist(clazz);

			for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
				Method readMethod = pd.getReadMethod();
				if (BLACKLIST.contains(pd.getName())) {
					continue;
				}
				if (readMethod == null || readMethod.getAnnotation(JsonIgnore.class) != null) {
					continue;
				}
				if (blacklist.contains(pd.getName())) {
					continue;
				}
				if (readMethod.getDeclaringClass() != clazz) {
					continue;
				}
				ClassBean clazzBean = getPropertyType(clazz, pd, mapper, tsPathMapper);

				com.sinnerschrader.aem.react.tsgenerator.descriptor.PropertyDescriptor pdd = com.sinnerschrader.aem.react.tsgenerator.descriptor.PropertyDescriptor
						.builder()//
						.name(pd.getName())//
						.type(convertType(pd.getPropertyType(), clazzBean, mapper))//
						.build();

				builder.property(pdd.getName(), pdd);
			}

			return builder
					.extendedInterfaces(tsInterfaces)
					.build();
		} catch (IntrospectionException e) {
			return null;
		}
	}

	private static Map<String, ExtendedInterface> collectTsInterfaces(Class<?> clazz,
			List<TsElementDefault> tsElementDefaults, PathMapper mapper, TsPathMapper tsPathMapper) {

		final Map<String, TsElementDefault> tsElementDefaultMap;
		if (tsElementDefaults == null || tsElementDefaults.isEmpty()) {
			tsElementDefaultMap = Collections.emptyMap();
		} else {
			tsElementDefaultMap = tsElementDefaults.stream()
					.collect(Collectors.toMap(TsElementDefault::getClassName, Function.identity()));
		}
		Map<String, ExtendedInterface> tsInterfaces = new HashMap<>();
		Class<?>[] extendedInterfaces = clazz.getInterfaces();
		if (extendedInterfaces != null) {
			for (int i = 0, len = extendedInterfaces.length; i < len; i++) {
				Class<?> extendedInterface = extendedInterfaces[i];
				TsElementDefault tsElementDefault = tsElementDefaultMap.get(extendedInterface.getCanonicalName());
				final String name;
				final String path;
				if (tsElementDefault == null) {
					name = extendedInterface.getSimpleName();
					path = mapper.apply(extendedInterface.getName());
				} else {
					name = StringUtils.isEmpty(tsElementDefault.getName())
						? extendedInterface.getSimpleName()
						: tsElementDefault.getName();
					path = tsPathMapper.apply(tsElementDefault.getPath());
				}
				tsInterfaces.put(name, new ExtendedInterface(name, path));
			}
		}
		for (AnnotatedType annotatedType : clazz.getAnnotatedInterfaces()) {
			TsElement tsElement = annotatedType.getAnnotation(TsElement.class);
			if (tsElement == null) {
				continue;
			}
			ClassBean classBean = new ClassBean(tsElement.value(), tsElement.name(), tsPathMapper);
			final String name = classBean.getSimpleName();
			final String path = tsPathMapper.apply(tsElement.value());
			tsInterfaces.put(name, new ExtendedInterface(name, path));
		}
		return tsInterfaces;
	}

	private static Set<String> createClassBlacklist(Class<?> clazz) {
		List<Class<?>> interfaceClasses = ClassUtils.getAllInterfaces(clazz);
		final Set<String> blacklist = new HashSet<>();
		for (Iterator<Class<?>> it = iteratorOf(interfaceClasses); it.hasNext(); ) {
			String interfaceClassName = it.next().getCanonicalName();
			Set<String> additionalExcludes = BLACKLIST_BY_CLASSNAME.get(interfaceClassName);
			if (additionalExcludes != null) {
				blacklist.addAll(additionalExcludes);
			}
		}
		return blacklist;
	}

	private static <E> Iterator<E> iteratorOf(Collection<E> collection) {
		if (collection == null) {
			collection = Collections.emptyList();
		}
		return collection.iterator();
	}

	private static ClassBean getPropertyType(Class clazz, PropertyDescriptor pd, PathMapper mapper,
			TsPathMapper tsPathMapper) {
		TsElement tsElement = getAnnotation(clazz, pd, TsElement.class);
		if (tsElement != null) {
			return new ClassBean(tsElement.value(), tsElement.name(), tsPathMapper);
		}
		Element element = getAnnotation(clazz, pd, Element.class);
		if (element != null) {
			return new ClassBean(element.value(), mapper);
		}
		return null;
	}

	private static <A extends Annotation> A getAnnotation(Class clazz, PropertyDescriptor pd, Class<A> aClass) {
		Method readMethod = pd.getReadMethod();
		A getterAnnotation = readMethod != null ? readMethod.getAnnotation(aClass) : null;
		if (getterAnnotation != null) {
			return getterAnnotation;
		}

		A fieldAnnotation = null;
		try {
			Field declaredField = clazz.getDeclaredField(pd.getName());
			fieldAnnotation = declaredField.getAnnotation(aClass);

		} catch (NoSuchFieldException | SecurityException e) {
			//
		}
		return fieldAnnotation;
	}

	public static ClassDescriptor createClassDescriptor(String clazzName, ScanContext ctx,
			List<TsElementDefault> tsElementDefaults, PathMapper mapper,  TsPathMapper tsPathMapper) {
		try {
			return createClassDescriptor(Class.forName(clazzName), ctx, tsElementDefaults, mapper, tsPathMapper);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("error", e);
		}
	}

	public static <E extends Enum<E>> EnumDescriptor createEnumDescriptor(Class<E> enumClass) {
		Map<String, E> map = EnumUtils.getEnumMap(enumClass);
		List<String> names = map.values().stream()
				.map(Enum::name)
				.sorted()
				.collect(Collectors.toList());

		return EnumDescriptor.builder()//
				.name(enumClass.getSimpleName())//
				.fullJavaClassName(enumClass.getName())//
				.values(names).build();
	}

}
