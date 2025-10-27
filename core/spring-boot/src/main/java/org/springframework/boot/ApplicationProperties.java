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

package org.springframework.boot;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Spring application properties.
 *
 * @author Moritz Halbritter
 */
class ApplicationProperties {

	/**
	 * Whether bean definition overriding, by registering a definition with the same name
	 * as an existing definition, is allowed.
	 */
	private boolean allowBeanDefinitionOverriding;

	/**
	 * Whether to allow circular references between beans and automatically try to resolve
	 * them.
	 */
	private boolean allowCircularReferences;

	/**
	 * Mode used to display the banner when the application runs.
	 */
	private Banner.@Nullable Mode bannerMode;

	/**
	 * Whether to keep the application alive even if there are no more non-daemon threads.
	 */
	private boolean keepAlive;

	/**
	 * Whether initialization should be performed lazily.
	 */
	private boolean lazyInitialization;

	/**
	 * Whether to log information about the application when it starts.
	 */
	private boolean logStartupInfo = true;

	/**
	 * Whether the application should have a shutdown hook registered.
	 */
	private boolean registerShutdownHook = true;

	/**
	 * Sources (class names, package names, or XML resource locations) to include in the
	 * ApplicationContext.
	 */
	private Set<String> sources = new LinkedHashSet<>();

	/**
	 * Flag to explicitly request a specific type of web application. If not set,
	 * auto-detected based on the classpath.
	 */
	private @Nullable WebApplicationType webApplicationType;

	boolean isAllowBeanDefinitionOverriding() {
		return this.allowBeanDefinitionOverriding;
	}

	void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	boolean isAllowCircularReferences() {
		return this.allowCircularReferences;
	}

	void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}

	Mode getBannerMode(Environment environment) {
		if (this.bannerMode != null) {
			return this.bannerMode;
		}
		String applicationPropertyName = LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT.getApplicationPropertyName();
		Assert.state(applicationPropertyName != null, "applicationPropertyName must not be null");
		boolean structuredLoggingEnabled = environment.containsProperty(applicationPropertyName);
		return (structuredLoggingEnabled) ? Mode.OFF : Banner.Mode.CONSOLE;
	}

	void setBannerMode(@Nullable Mode bannerMode) {
		this.bannerMode = bannerMode;
	}

	boolean isKeepAlive() {
		return this.keepAlive;
	}

	void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	boolean isLazyInitialization() {
		return this.lazyInitialization;
	}

	void setLazyInitialization(boolean lazyInitialization) {
		this.lazyInitialization = lazyInitialization;
	}

	boolean isLogStartupInfo() {
		return this.logStartupInfo;
	}

	void setLogStartupInfo(boolean logStartupInfo) {
		this.logStartupInfo = logStartupInfo;
	}

	boolean isRegisterShutdownHook() {
		return this.registerShutdownHook;
	}

	void setRegisterShutdownHook(boolean registerShutdownHook) {
		this.registerShutdownHook = registerShutdownHook;
	}

	Set<String> getSources() {
		return this.sources;
	}

	void setSources(Set<String> sources) {
		this.sources = new LinkedHashSet<>(sources);
	}

	@Nullable WebApplicationType getWebApplicationType() {
		return this.webApplicationType;
	}

	void setWebApplicationType(@Nullable WebApplicationType webApplicationType) {
		this.webApplicationType = webApplicationType;
	}

	static class ApplicationPropertiesRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			BindableRuntimeHintsRegistrar.forTypes(ApplicationProperties.class).registerHints(hints, classLoader);
		}

	}

}
