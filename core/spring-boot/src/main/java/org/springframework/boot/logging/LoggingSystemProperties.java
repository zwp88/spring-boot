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

package org.springframework.boot.logging;

import java.io.Console;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.system.ApplicationPid;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility to set system properties that can later be used by log configuration files.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 * @since 2.0.0
 * @see LoggingSystemProperty
 */
public class LoggingSystemProperties {

	private static final BiConsumer<String, @Nullable String> systemPropertySetter = (name, value) -> {
		if (System.getProperty(name) == null && value != null) {
			System.setProperty(name, value);
		}
	};

	private final Environment environment;

	private final Function<@Nullable String, @Nullable String> defaultValueResolver;

	private final BiConsumer<String, @Nullable String> setter;

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 */
	public LoggingSystemProperties(Environment environment) {
		this(environment, null);
	}

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param setter setter used to apply the property or {@code null} for system
	 * properties
	 * @since 2.4.2
	 */
	public LoggingSystemProperties(Environment environment, @Nullable BiConsumer<String, @Nullable String> setter) {
		this(environment, null, setter);
	}

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param defaultValueResolver function used to resolve default values or {@code null}
	 * @param setter setter used to apply the property or {@code null} for system
	 * properties
	 * @since 3.2.0
	 */
	public LoggingSystemProperties(Environment environment,
			@Nullable Function<@Nullable String, @Nullable String> defaultValueResolver,
			@Nullable BiConsumer<String, @Nullable String> setter) {
		Assert.notNull(environment, "'environment' must not be null");
		this.environment = environment;
		this.defaultValueResolver = (defaultValueResolver != null) ? defaultValueResolver : (name) -> null;
		this.setter = (setter != null) ? setter : systemPropertySetter;
	}

	/**
	 * Returns the {@link Console} to use.
	 * @return the {@link Console} to use
	 * @since 3.5.0
	 */
	protected @Nullable Console getConsole() {
		return System.console();
	}

	public final void apply() {
		apply(null);
	}

	public final void apply(@Nullable LogFile logFile) {
		PropertyResolver resolver = getPropertyResolver();
		apply(logFile, resolver);
	}

	private PropertyResolver getPropertyResolver() {
		if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
			PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
					configurableEnvironment.getPropertySources());
			resolver.setConversionService(configurableEnvironment.getConversionService());
			resolver.setIgnoreUnresolvableNestedPlaceholders(true);
			return resolver;
		}
		return this.environment;
	}

	protected void apply(@Nullable LogFile logFile, PropertyResolver resolver) {
		setSystemProperty(LoggingSystemProperty.APPLICATION_NAME, resolver);
		setSystemProperty(LoggingSystemProperty.APPLICATION_GROUP, resolver);
		setSystemProperty(LoggingSystemProperty.PID, new ApplicationPid().toString());
		setSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET, resolver, getDefaultConsoleCharset().name());
		setSystemProperty(LoggingSystemProperty.FILE_CHARSET, resolver, getDefaultFileCharset().name());
		setSystemProperty(LoggingSystemProperty.CONSOLE_THRESHOLD, resolver, this::thresholdMapper);
		setSystemProperty(LoggingSystemProperty.FILE_THRESHOLD, resolver, this::thresholdMapper);
		setSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD, resolver);
		setSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.FILE_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT, resolver);
		setSystemProperty(LoggingSystemProperty.FILE_STRUCTURED_FORMAT, resolver);
		setSystemProperty(LoggingSystemProperty.LEVEL_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.DATEFORMAT_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.CORRELATION_PATTERN, resolver);
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
		if (!this.environment.getProperty("logging.console.enabled", Boolean.class, true)) {
			setSystemProperty(LoggingSystemProperty.CONSOLE_THRESHOLD.getEnvironmentVariableName(), "OFF");
		}
	}

	/**
	 * Returns the default console charset.
	 * @return the default console charset
	 * @since 3.5.0
	 */
	protected Charset getDefaultConsoleCharset() {
		Console console = getConsole();
		return (console != null) ? console.charset() : Charset.defaultCharset();
	}

	/**
	 * Returns the default file charset.
	 * @return the default file charset
	 * @since 3.5.0
	 */
	protected Charset getDefaultFileCharset() {
		return StandardCharsets.UTF_8;
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver) {
		setSystemProperty(property, resolver, (i) -> i);
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver,
			Function<@Nullable String, @Nullable String> mapper) {
		setSystemProperty(property, resolver, null, mapper);
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver, String defaultValue) {
		setSystemProperty(property, resolver, defaultValue, (i) -> i);
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver,
			@Nullable String defaultValue, Function<@Nullable String, @Nullable String> mapper) {
		if (property.getIncludePropertyName() != null) {
			if (!resolver.getProperty(property.getIncludePropertyName(), Boolean.class, Boolean.TRUE)) {
				return;
			}
		}
		String applicationPropertyName = property.getApplicationPropertyName();
		String value = (applicationPropertyName != null) ? resolver.getProperty(applicationPropertyName) : null;
		value = (value != null) ? value : this.defaultValueResolver.apply(applicationPropertyName);
		value = (value != null) ? value : defaultValue;
		value = mapper.apply(value);
		setSystemProperty(property.getEnvironmentVariableName(), value);
		if (property == LoggingSystemProperty.APPLICATION_NAME && StringUtils.hasText(value)) {
			// LOGGED_APPLICATION_NAME is deprecated for removal in 4.0.0
			setSystemProperty("LOGGED_APPLICATION_NAME", "[%s] ".formatted(value));
		}
	}

	private void setSystemProperty(LoggingSystemProperty property, String value) {
		setSystemProperty(property.getEnvironmentVariableName(), value);
	}

	private @Nullable String thresholdMapper(@Nullable String input) {
		// YAML converts an unquoted OFF to false
		if ("false".equals(input)) {
			return "OFF";
		}
		return input;
	}

	/**
	 * Set a system property.
	 * @param name the property name
	 * @param value the value
	 */
	protected final void setSystemProperty(String name, @Nullable String value) {
		this.setter.accept(name, value);
	}

}
