/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.CollectionFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * {@link DataObjectBinder} for immutable value objects.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ValueObjectBinder implements DataObjectBinder {

	private static final Log logger = LogFactory.getLog(ValueObjectBinder.class);

	private final BindConstructorProvider constructorProvider;

	ValueObjectBinder(BindConstructorProvider constructorProvider) {
		this.constructorProvider = constructorProvider;
	}

	@Override
	public <T> @Nullable T bind(ConfigurationPropertyName name, Bindable<T> target, Binder.Context context,
			DataObjectPropertyBinder propertyBinder) {
		ValueObject<T> valueObject = ValueObject.get(target, context, this.constructorProvider, Discoverer.LENIENT);
		if (valueObject == null) {
			return null;
		}
		Class<?> targetType = target.getType().resolve();
		Assert.state(targetType != null, "'targetType' must not be null");
		context.pushConstructorBoundTypes(targetType);
		List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
		List<@Nullable Object> args = new ArrayList<>(parameters.size());
		boolean bound = false;
		for (ConstructorParameter parameter : parameters) {
			Object arg = parameter.bind(propertyBinder);
			bound = bound || arg != null;
			arg = (arg != null) ? arg : getDefaultValue(context, parameter);
			args.add(arg);
		}
		context.clearConfigurationProperty();
		context.popConstructorBoundTypes();
		return bound ? valueObject.instantiate(args) : null;
	}

	@Override
	public <T> @Nullable T create(Bindable<T> target, Binder.Context context) {
		ValueObject<T> valueObject = ValueObject.get(target, context, this.constructorProvider, Discoverer.LENIENT);
		if (valueObject == null) {
			return null;
		}
		List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
		List<@Nullable Object> args = new ArrayList<>(parameters.size());
		for (ConstructorParameter parameter : parameters) {
			args.add(getDefaultValue(context, parameter));
		}
		return valueObject.instantiate(args);
	}

	@Override
	public <T> void onUnableToCreateInstance(Bindable<T> target, Context context, RuntimeException exception) {
		try {
			ValueObject.get(target, context, this.constructorProvider, Discoverer.STRICT);
		}
		catch (Exception ex) {
			exception.addSuppressed(ex);
		}
	}

	private <T> @Nullable T getDefaultValue(Binder.Context context, ConstructorParameter parameter) {
		ResolvableType type = parameter.getType();
		Annotation[] annotations = parameter.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation instanceof DefaultValue defaultValueAnnotation) {
				String[] defaultValue = defaultValueAnnotation.value();
				if (defaultValue.length == 0) {
					return getNewDefaultValueInstanceIfPossible(context, type);
				}
				return convertDefaultValue(context.getConverter(), defaultValue, type, annotations);
			}
		}
		return null;
	}

	private <T> @Nullable T convertDefaultValue(BindConverter converter, String[] defaultValue, ResolvableType type,
			Annotation[] annotations) {
		try {
			return converter.convert(defaultValue, type, annotations);
		}
		catch (ConversionException ex) {
			// Try again in case ArrayToObjectConverter is not in play
			if (defaultValue.length == 1) {
				return converter.convert(defaultValue[0], type, annotations);
			}
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> @Nullable T getNewDefaultValueInstanceIfPossible(Binder.Context context, ResolvableType type) {
		Class<T> resolved = (Class<T>) type.resolve();
		Assert.state(resolved == null || isEmptyDefaultValueAllowed(resolved),
				() -> "Parameter of type " + type + " must have a non-empty default value.");
		if (resolved != null) {
			if (Optional.class == resolved) {
				return (T) Optional.empty();
			}
			if (Collection.class.isAssignableFrom(resolved)) {
				return (T) CollectionFactory.createCollection(resolved, 0);
			}
			if (EnumMap.class.isAssignableFrom(resolved)) {
				Class<?> keyType = type.asMap().resolveGeneric(0);
				return (T) CollectionFactory.createMap(resolved, keyType, 0);
			}
			if (Map.class.isAssignableFrom(resolved)) {
				return (T) CollectionFactory.createMap(resolved, 0);
			}
			if (resolved.isArray()) {
				return (T) Array.newInstance(resolved.getComponentType(), 0);
			}
		}
		T instance = create(Bindable.of(type), context);
		if (instance != null) {
			return instance;
		}
		return (resolved != null) ? BeanUtils.instantiateClass(resolved) : null;
	}

	private boolean isEmptyDefaultValueAllowed(Class<?> type) {
		return (Optional.class == type || isAggregate(type))
				|| !(type.isPrimitive() || type.isEnum() || type.getName().startsWith("java.lang"));
	}

	private boolean isAggregate(Class<?> type) {
		return type.isArray() || Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
	}

	/**
	 * The value object being bound.
	 *
	 * @param <T> the value object type
	 */
	private abstract static class ValueObject<T> {

		private static final Object NONE = new Object();

		private final Constructor<T> constructor;

		protected ValueObject(Constructor<T> constructor) {
			this.constructor = constructor;
		}

		T instantiate(List<@Nullable Object> args) {
			return BeanUtils.instantiateClass(this.constructor, args.toArray());
		}

		abstract List<ConstructorParameter> getConstructorParameters();

		@SuppressWarnings("unchecked")
		static <T> @Nullable ValueObject<T> get(Bindable<T> bindable, Binder.Context context,
				BindConstructorProvider constructorProvider, ParameterNameDiscoverer parameterNameDiscoverer) {
			Class<T> resolvedType = (Class<T>) bindable.getType().resolve();
			if (resolvedType == null || resolvedType.isEnum() || Modifier.isAbstract(resolvedType.getModifiers())) {
				return null;
			}
			Map<CacheKey, Object> cache = getCache(context);
			CacheKey cacheKey = new CacheKey(bindable, constructorProvider, parameterNameDiscoverer);
			Object valueObject = cache.get(cacheKey);
			if (valueObject == null) {
				valueObject = get(bindable, context, constructorProvider, parameterNameDiscoverer, resolvedType);
				cache.put(cacheKey, (valueObject != null) ? valueObject : NONE);
			}
			return (valueObject != NONE) ? (ValueObject<T>) valueObject : null;
		}

		@SuppressWarnings("unchecked")
		private static <T> @Nullable ValueObject<T> get(Bindable<T> bindable, Binder.Context context,
				BindConstructorProvider constructorProvider, ParameterNameDiscoverer parameterNameDiscoverer,
				Class<T> resolvedType) {
			Constructor<?> bindConstructor = constructorProvider.getBindConstructor(bindable,
					context.isNestedConstructorBinding());
			if (bindConstructor == null) {
				return null;
			}
			if (KotlinDetector.isKotlinType(resolvedType)) {
				return KotlinValueObject.get((Constructor<T>) bindConstructor, bindable.getType(),
						parameterNameDiscoverer);
			}
			return DefaultValueObject.get(bindConstructor, bindable.getType(), parameterNameDiscoverer);
		}

		@SuppressWarnings("unchecked")
		private static Map<CacheKey, Object> getCache(Context context) {
			Map<CacheKey, Object> cache = (Map<CacheKey, Object>) context.getCache().get(ValueObject.class);
			if (cache == null) {
				cache = new ConcurrentHashMap<>();
				context.getCache().put(ValueObject.class, cache);
			}
			return cache;
		}

		private record CacheKey(Bindable<?> bindable, BindConstructorProvider constructorProvider,
				ParameterNameDiscoverer parameterNameDiscoverer) {

		}

	}

	/**
	 * A {@link ValueObject} implementation that is aware of Kotlin specific constructs.
	 */
	private static final class KotlinValueObject<T> extends ValueObject<T> {

		private static final Annotation[] ANNOTATION_ARRAY = new Annotation[0];

		private final List<ConstructorParameter> constructorParameters;

		private KotlinValueObject(Constructor<T> primaryConstructor, KFunction<T> kotlinConstructor,
				ResolvableType type) {
			super(primaryConstructor);
			this.constructorParameters = parseConstructorParameters(kotlinConstructor, type);
		}

		private List<ConstructorParameter> parseConstructorParameters(KFunction<T> kotlinConstructor,
				ResolvableType type) {
			List<KParameter> parameters = kotlinConstructor.getParameters();
			List<ConstructorParameter> result = new ArrayList<>(parameters.size());
			for (KParameter parameter : parameters) {
				String name = getParameterName(parameter);
				ResolvableType parameterType = ResolvableType
					.forType(ReflectJvmMapping.getJavaType(parameter.getType()), type);
				Annotation[] annotations = parameter.getAnnotations().toArray(ANNOTATION_ARRAY);
				Assert.state(name != null, "'name' must not be null");
				result.add(new ConstructorParameter(name, parameterType, annotations));
			}
			return Collections.unmodifiableList(result);
		}

		private @Nullable String getParameterName(KParameter parameter) {
			return MergedAnnotations.from(parameter, parameter.getAnnotations().toArray(ANNOTATION_ARRAY))
				.get(Name.class)
				.getValue(MergedAnnotation.VALUE, String.class)
				.orElseGet(parameter::getName);
		}

		@Override
		List<ConstructorParameter> getConstructorParameters() {
			return this.constructorParameters;
		}

		static <T> @Nullable ValueObject<T> get(Constructor<T> bindConstructor, ResolvableType type,
				ParameterNameDiscoverer parameterNameDiscoverer) {
			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(bindConstructor);
			if (kotlinConstructor != null) {
				return new KotlinValueObject<>(bindConstructor, kotlinConstructor, type);
			}
			return DefaultValueObject.get(bindConstructor, type, parameterNameDiscoverer);
		}

	}

	/**
	 * A default {@link ValueObject} implementation that uses only standard Java
	 * reflection calls.
	 */
	private static final class DefaultValueObject<T> extends ValueObject<T> {

		private final List<ConstructorParameter> constructorParameters;

		private DefaultValueObject(Constructor<T> constructor, List<ConstructorParameter> constructorParameters) {
			super(constructor);
			this.constructorParameters = constructorParameters;
		}

		@Override
		List<ConstructorParameter> getConstructorParameters() {
			return this.constructorParameters;
		}

		@SuppressWarnings("unchecked")
		static <T> @Nullable ValueObject<T> get(Constructor<?> bindConstructor, ResolvableType type,
				ParameterNameDiscoverer parameterNameDiscoverer) {
			@Nullable String @Nullable [] names = parameterNameDiscoverer.getParameterNames(bindConstructor);
			if (names == null) {
				return null;
			}
			List<ConstructorParameter> constructorParameters = parseConstructorParameters(bindConstructor, type, names);
			return new DefaultValueObject<>((Constructor<T>) bindConstructor, constructorParameters);
		}

		private static List<ConstructorParameter> parseConstructorParameters(Constructor<?> constructor,
				ResolvableType type, @Nullable String[] names) {
			Parameter[] parameters = constructor.getParameters();
			List<ConstructorParameter> result = new ArrayList<>(parameters.length);
			for (int i = 0; i < parameters.length; i++) {
				String name = MergedAnnotations.from(parameters[i])
					.get(Name.class)
					.getValue(MergedAnnotation.VALUE, String.class)
					.orElse(names[i]);
				ResolvableType parameterType = ResolvableType.forMethodParameter(new MethodParameter(constructor, i),
						type);
				Annotation[] annotations = parameters[i].getDeclaredAnnotations();
				Assert.state(name != null, "'name' must not be null");
				result.add(new ConstructorParameter(name, parameterType, annotations));
			}
			return Collections.unmodifiableList(result);
		}

	}

	/**
	 * A constructor parameter being bound.
	 */
	private static class ConstructorParameter {

		private final String name;

		private final ResolvableType type;

		private final Annotation[] annotations;

		ConstructorParameter(String name, ResolvableType type, Annotation[] annotations) {
			this.name = DataObjectPropertyName.toDashedForm(name);
			this.type = type;
			this.annotations = annotations;
		}

		@Nullable Object bind(DataObjectPropertyBinder propertyBinder) {
			return propertyBinder.bindProperty(this.name, Bindable.of(this.type).withAnnotations(this.annotations));
		}

		Annotation[] getAnnotations() {
			return this.annotations;
		}

		ResolvableType getType() {
			return this.type;
		}

	}

	/**
	 * {@link ParameterNameDiscoverer} used for value data object binding.
	 */
	static final class Discoverer implements ParameterNameDiscoverer {

		private static final ParameterNameDiscoverer DEFAULT_DELEGATE = new DefaultParameterNameDiscoverer();

		private static final ParameterNameDiscoverer LENIENT = new Discoverer(DEFAULT_DELEGATE, (message) -> {
		});

		private static final ParameterNameDiscoverer STRICT = new Discoverer(DEFAULT_DELEGATE, (message) -> {
			throw new IllegalStateException(message.toString());
		});

		private final ParameterNameDiscoverer delegate;

		private final Consumer<LogMessage> noParameterNamesHandler;

		private Discoverer(ParameterNameDiscoverer delegate, Consumer<LogMessage> noParameterNamesHandler) {
			this.delegate = delegate;
			this.noParameterNamesHandler = noParameterNamesHandler;
		}

		@Override
		public String[] getParameterNames(Method method) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable String @Nullable [] getParameterNames(Constructor<?> constructor) {
			@Nullable String @Nullable [] names = this.delegate.getParameterNames(constructor);
			if (names != null) {
				return names;
			}
			LogMessage message = LogMessage.format(
					"Unable to use value object binding with constructor [%s] as parameter names cannot be discovered. "
							+ "Ensure that the compiler uses the '-parameters' flag",
					constructor);
			this.noParameterNamesHandler.accept(message);
			logger.debug(message);
			return null;
		}

	}

}
