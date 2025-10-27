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

package org.springframework.boot.servlet.autoconfigure;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for multipart uploads. Adds a
 * {@link StandardServletMultipartResolver} if none is present, and adds a
 * {@link jakarta.servlet.MultipartConfigElement multipartConfigElement} if none is
 * otherwise defined.
 * <p>
 * The {@link jakarta.servlet.MultipartConfigElement} is a Servlet API that's used to
 * configure how the server handles file uploads.
 *
 * @author Greg Turnquist
 * @author Josh Long
 * @author Toshiaki Maki
 * @author Yanming Zhou
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ Servlet.class, StandardServletMultipartResolver.class, MultipartConfigElement.class })
@ConditionalOnBooleanProperty(name = "spring.servlet.multipart.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(MultipartProperties.class)
public final class MultipartAutoConfiguration {

	/**
	 * Well-known name for the MultipartResolver object in the bean factory for this
	 * namespace.
	 */
	private static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

	private final MultipartProperties multipartProperties;

	MultipartAutoConfiguration(MultipartProperties multipartProperties) {
		this.multipartProperties = multipartProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	MultipartConfigElement multipartConfigElement() {
		return this.multipartProperties.createMultipartConfig();
	}

	@Bean(name = MULTIPART_RESOLVER_BEAN_NAME)
	@ConditionalOnMissingBean(MultipartResolver.class)
	StandardServletMultipartResolver multipartResolver() {
		StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
		multipartResolver.setResolveLazily(this.multipartProperties.isResolveLazily());
		multipartResolver.setStrictServletCompliance(this.multipartProperties.isStrictServletCompliance());
		return multipartResolver;
	}

}
