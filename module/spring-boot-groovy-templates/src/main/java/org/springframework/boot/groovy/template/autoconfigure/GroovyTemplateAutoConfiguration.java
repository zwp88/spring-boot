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

package org.springframework.boot.groovy.template.autoconfigure;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import groovy.text.markup.MarkupTemplateEngine;
import jakarta.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.core.log.LogMessage;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfig;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

/**
 * Auto-configuration support for Groovy templates in MVC. By default creates a
 * {@link MarkupTemplateEngine} configured from {@link GroovyTemplateProperties}, but you
 * can override that by providing your own {@link GroovyMarkupConfig} or even a
 * {@link MarkupTemplateEngine} of a different type.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(MarkupTemplateEngine.class)
@EnableConfigurationProperties(GroovyTemplateProperties.class)
public final class GroovyTemplateAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GroovyTemplateAutoConfiguration.class);

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(GroovyMarkupConfigurer.class)
	static class GroovyMarkupConfiguration {

		private final ApplicationContext applicationContext;

		private final GroovyTemplateProperties properties;

		GroovyMarkupConfiguration(ApplicationContext applicationContext, GroovyTemplateProperties properties) {
			this.applicationContext = applicationContext;
			this.properties = properties;
			checkTemplateLocationExists();
		}

		private void checkTemplateLocationExists() {
			if (this.properties.isCheckTemplateLocation() && !isUsingGroovyAllJar()) {
				TemplateLocation location = new TemplateLocation(this.properties.getResourceLoaderPath());
				if (!location.exists(this.applicationContext)) {
					logger.warn(LogMessage.format(
							"Cannot find template location: %s (please add some templates, check your Groovy "
									+ "configuration, or set spring.groovy.template.check-template-location=false)",
							location));
				}
			}
		}

		/**
		 * MarkupTemplateEngine could be loaded from groovy-templates or groovy-all.
		 * Unfortunately it's quite common for people to use groovy-all and not actually
		 * need templating support. This method attempts to check the source jar so that
		 * we can skip the {@code /template} directory check for such cases.
		 * @return true if the groovy-all jar is used
		 */
		private boolean isUsingGroovyAllJar() {
			try {
				ProtectionDomain domain = MarkupTemplateEngine.class.getProtectionDomain();
				CodeSource codeSource = domain.getCodeSource();
				return codeSource != null && codeSource.getLocation().toString().contains("-all");
			}
			catch (Exception ex) {
				return false;
			}
		}

		@Bean
		@ConditionalOnMissingBean(GroovyMarkupConfig.class)
		GroovyMarkupConfigurer groovyMarkupConfigurer(ObjectProvider<MarkupTemplateEngine> templateEngine,
				Environment environment) {
			GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
			PropertyMapper map = PropertyMapper.get();
			map.from(this.properties::isAutoEscape).to(configurer::setAutoEscape);
			map.from(this.properties::isAutoIndent).to(configurer::setAutoIndent);
			map.from(this.properties::getAutoIndentString).to(configurer::setAutoIndentString);
			map.from(this.properties::isAutoNewLine).to(configurer::setAutoNewLine);
			map.from(this.properties::getBaseTemplateClass).to(configurer::setBaseTemplateClass);
			map.from(this.properties::isCache).to(configurer::setCacheTemplates);
			map.from(this.properties::getDeclarationEncoding).to(configurer::setDeclarationEncoding);
			map.from(this.properties::isExpandEmptyElements).to(configurer::setExpandEmptyElements);
			map.from(this.properties::getLocale).to(configurer::setLocale);
			map.from(this.properties::getNewLineString).to(configurer::setNewLineString);
			map.from(this.properties::getResourceLoaderPath).to(configurer::setResourceLoaderPath);
			map.from(this.properties::isUseDoubleQuotes).to(configurer::setUseDoubleQuotes);
			Binder.get(environment).bind("spring.groovy.template.configuration", Bindable.ofInstance(configurer));
			templateEngine.ifAvailable(configurer::setTemplateEngine);
			return configurer;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Servlet.class, LocaleContextHolder.class, UrlBasedViewResolver.class })
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnBooleanProperty(name = "spring.groovy.template.enabled", matchIfMissing = true)
	static class GroovyWebConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "groovyMarkupViewResolver")
		GroovyMarkupViewResolver groovyMarkupViewResolver(GroovyTemplateProperties properties) {
			GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
			properties.applyToMvcViewResolver(resolver);
			return resolver;
		}

	}

}
