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

package org.springframework.boot.jackson2.autoconfigure;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson2.JsonComponentModule;
import org.springframework.boot.jackson2.JsonMixinModule;
import org.springframework.boot.jackson2.JsonMixinModuleEntries;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Auto configuration for Jackson. The following auto-configuration will get applied:
 * <ul>
 * <li>an {@link ObjectMapper} in case none is already configured.</li>
 * <li>a {@link org.springframework.http.converter.json.Jackson2ObjectMapperBuilder} in
 * case none is already configured.</li>
 * <li>auto-registration for all {@link Module} beans with all {@link ObjectMapper} beans
 * (including the defaulted ones).</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Sebastien Deleuze
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Ralf Ueberfuhr
 * @since 4.0.0
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of Jackson 3.
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public final class Jackson2AutoConfiguration {

	private static final Map<?, Boolean> FEATURE_DEFAULTS;

	static {
		Map<Object, Boolean> featureDefaults = new HashMap<>();
		featureDefaults.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		featureDefaults.put(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
		FEATURE_DEFAULTS = Collections.unmodifiableMap(featureDefaults);
	}

	@Bean
	JsonComponentModule jackson2JsonComponentModule() {
		return new JsonComponentModule();
	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonMixinConfiguration {

		@Bean
		static JsonMixinModuleEntries jackson2JsonMixinModuleEntries(ApplicationContext context) {
			List<String> packages = AutoConfigurationPackages.has(context) ? AutoConfigurationPackages.get(context)
					: Collections.emptyList();
			return JsonMixinModuleEntries.scan(context, packages);
		}

		@Bean
		JsonMixinModule jackson2JsonMixinModule(ApplicationContext context, JsonMixinModuleEntries entries) {
			JsonMixinModule jsonMixinModule = new JsonMixinModule();
			jsonMixinModule.registerEntries(entries, context.getClassLoader());
			return jsonMixinModule;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.class)
	static class JacksonObjectMapperConfiguration {

		@Bean
		@Primary
		@ConditionalOnMissingBean
		ObjectMapper jackson2ObjectMapper(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
			return builder.createXmlMapper(false).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ParameterNamesModule.class)
	static class ParameterNamesModuleConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ParameterNamesModule jackson2ParameterNamesModule() {
			return new ParameterNamesModule(JsonCreator.Mode.DEFAULT);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.class)
	static class JacksonObjectMapperBuilderConfiguration {

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		@ConditionalOnMissingBean
		org.springframework.http.converter.json.Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder(
				ApplicationContext applicationContext, List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
			org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder = new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
			builder.applicationContext(applicationContext);
			customize(builder, customizers);
			return builder;
		}

		private void customize(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder,
				List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
			for (Jackson2ObjectMapperBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.class)
	@EnableConfigurationProperties(Jackson2Properties.class)
	static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

		@Bean
		StandardJackson2ObjectMapperBuilderCustomizer standardJackson2ObjectMapperBuilderCustomizer(
				Jackson2Properties jacksonProperties, ObjectProvider<Module> modules) {
			return new StandardJackson2ObjectMapperBuilderCustomizer(jacksonProperties, modules.stream().toList());
		}

		static final class StandardJackson2ObjectMapperBuilderCustomizer
				implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

			private final Jackson2Properties jacksonProperties;

			private final Collection<Module> modules;

			StandardJackson2ObjectMapperBuilderCustomizer(Jackson2Properties jacksonProperties,
					Collection<Module> modules) {
				this.jacksonProperties = jacksonProperties;
				this.modules = modules;
			}

			@Override
			public int getOrder() {
				return 0;
			}

			@Override
			public void customize(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				if (this.jacksonProperties.getDefaultPropertyInclusion() != null) {
					builder.serializationInclusion(this.jacksonProperties.getDefaultPropertyInclusion());
				}
				if (this.jacksonProperties.getTimeZone() != null) {
					builder.timeZone(this.jacksonProperties.getTimeZone());
				}
				configureFeatures(builder, FEATURE_DEFAULTS);
				configureVisibility(builder, this.jacksonProperties.getVisibility());
				configureFeatures(builder, this.jacksonProperties.getDeserialization());
				configureFeatures(builder, this.jacksonProperties.getSerialization());
				configureFeatures(builder, this.jacksonProperties.getMapper());
				configureFeatures(builder, this.jacksonProperties.getParser());
				configureFeatures(builder, this.jacksonProperties.getGenerator());
				configureFeatures(builder, this.jacksonProperties.getDatatype().getEnum());
				configureFeatures(builder, this.jacksonProperties.getDatatype().getJsonNode());
				configureDateFormat(builder);
				configurePropertyNamingStrategy(builder);
				configureModules(builder);
				configureLocale(builder);
				configureDefaultLeniency(builder);
				configureConstructorDetector(builder);
			}

			private void configureFeatures(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder,
					Map<?, Boolean> features) {
				features.forEach((feature, value) -> {
					if (value != null) {
						if (value) {
							builder.featuresToEnable(feature);
						}
						else {
							builder.featuresToDisable(feature);
						}
					}
				});
			}

			private void configureVisibility(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder,
					Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
				visibilities.forEach(builder::visibility);
			}

			private void configureDateFormat(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				// We support a fully qualified class name extending DateFormat or a date
				// pattern string value
				String dateFormat = this.jacksonProperties.getDateFormat();
				if (dateFormat != null) {
					try {
						Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
						builder.dateFormat((DateFormat) BeanUtils.instantiateClass(dateFormatClass));
					}
					catch (ClassNotFoundException ex) {
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
						// Since Jackson 2.6.3 we always need to set a TimeZone (see
						// gh-4170). If none in our properties fallback to the Jackson's
						// default
						TimeZone timeZone = this.jacksonProperties.getTimeZone();
						if (timeZone == null) {
							timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
						}
						simpleDateFormat.setTimeZone(timeZone);
						builder.dateFormat(simpleDateFormat);
					}
				}
			}

			private void configurePropertyNamingStrategy(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				// We support a fully qualified class name extending Jackson's
				// PropertyNamingStrategy or a string value corresponding to the constant
				// names in PropertyNamingStrategy which hold default provided
				// implementations
				String strategy = this.jacksonProperties.getPropertyNamingStrategy();
				if (strategy != null) {
					try {
						configurePropertyNamingStrategyClass(builder, ClassUtils.forName(strategy, null));
					}
					catch (ClassNotFoundException ex) {
						configurePropertyNamingStrategyField(builder, strategy);
					}
				}
			}

			private void configurePropertyNamingStrategyClass(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder,
					Class<?> propertyNamingStrategyClass) {
				builder.propertyNamingStrategy(
						(PropertyNamingStrategy) BeanUtils.instantiateClass(propertyNamingStrategyClass));
			}

			private void configurePropertyNamingStrategyField(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder, String fieldName) {
				// Find the field (this way we automatically support new constants
				// that may be added by Jackson in the future)
				Field field = findPropertyNamingStrategyField(fieldName);
				Assert.state(field != null, () -> "Constant named '" + fieldName + "' not found");
				try {
					builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}

			private Field findPropertyNamingStrategyField(String fieldName) {
				Field field = ReflectionUtils.findField(com.fasterxml.jackson.databind.PropertyNamingStrategies.class,
						fieldName, PropertyNamingStrategy.class);
				Assert.state(field != null, () -> "Constant named '" + fieldName + "' not found");
				return field;
			}

			private void configureModules(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				builder.modulesToInstall((modules) -> modules.addAll(this.modules));
			}

			private void configureLocale(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				Locale locale = this.jacksonProperties.getLocale();
				if (locale != null) {
					builder.locale(locale);
				}
			}

			private void configureDefaultLeniency(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				Boolean defaultLeniency = this.jacksonProperties.getDefaultLeniency();
				if (defaultLeniency != null) {
					builder.postConfigurer((objectMapper) -> objectMapper.setDefaultLeniency(defaultLeniency));
				}
			}

			private void configureConstructorDetector(
					org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
				org.springframework.boot.jackson2.autoconfigure.Jackson2Properties.ConstructorDetectorStrategy strategy = this.jacksonProperties
					.getConstructorDetector();
				if (strategy != null) {
					builder.postConfigurer((objectMapper) -> {
						switch (strategy) {
							case USE_PROPERTIES_BASED ->
								objectMapper.setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
							case USE_DELEGATING ->
								objectMapper.setConstructorDetector(ConstructorDetector.USE_DELEGATING);
							case EXPLICIT_ONLY ->
								objectMapper.setConstructorDetector(ConstructorDetector.EXPLICIT_ONLY);
							default -> objectMapper.setConstructorDetector(ConstructorDetector.DEFAULT);
						}
					});
				}
			}

		}

	}

	static class Jackson2AutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			if (ClassUtils.isPresent("com.fasterxml.jackson.databind.PropertyNamingStrategy", classLoader)) {
				registerPropertyNamingStrategyHints(hints.reflection());
			}
		}

		/**
		 * Register hints for the {@code configurePropertyNamingStrategyField} method to
		 * use.
		 * @param hints reflection hints
		 */
		private void registerPropertyNamingStrategyHints(ReflectionHints hints) {
			registerPropertyNamingStrategyHints(hints, PropertyNamingStrategies.class);
		}

		private void registerPropertyNamingStrategyHints(ReflectionHints hints, Class<?> type) {
			Stream.of(type.getDeclaredFields())
				.filter(this::isPropertyNamingStrategyField)
				.forEach(hints::registerField);
		}

		private boolean isPropertyNamingStrategyField(Field candidate) {
			return ReflectionUtils.isPublicStaticFinal(candidate)
					&& candidate.getType().isAssignableFrom(PropertyNamingStrategy.class);
		}

	}

}
