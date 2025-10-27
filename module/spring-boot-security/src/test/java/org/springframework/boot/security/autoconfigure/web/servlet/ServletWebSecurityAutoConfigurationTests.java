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

package org.springframework.boot.security.autoconfigure.web.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.interfaces.RSAPublicKey;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.servlet.filter.OrderedFilter;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.PathPatternRequestMatcherBuilderFactoryBean;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletWebSecurityAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class ServletWebSecurityAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
				ServletWebSecurityAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class));

	@Test
	void testWebConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(AuthenticationManagerBuilder.class)).isNotNull();
			assertThat(context.getBean(FilterChainProxy.class).getFilterChains()).hasSize(1);
		});
	}

	@Test
	void enableWebSecurityIsConditionalOnClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.security.config"))
			.run((context) -> assertThat(context).doesNotHaveBean("springSecurityFilterChain"));
	}

	@Test
	void filterChainBeanIsConditionalOnClassSecurityFilterChain() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SecurityFilterChain.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SecurityFilterChain.class));
	}

	@Test
	void securityConfigurerBacksOffWhenOtherSecurityFilterChainBeanPresent() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
			.withUserConfiguration(TestSecurityFilterChainConfig.class)
			.run((context) -> {
				assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
				assertThat(context.containsBean("testSecurityFilterChain")).isTrue();
			});
	}

	@Test
	void testFilterIsNotRegisteredInNonWeb() {
		try (AnnotationConfigApplicationContext customContext = new AnnotationConfigApplicationContext()) {
			customContext.register(SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			customContext.refresh();
			assertThat(customContext.containsBean("securityFilterChainRegistration")).isFalse();
		}
	}

	@Test
	void testDefaultFilterOrder() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> assertThat(
					context.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class)
						.getOrder())
				.isEqualTo(OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100));
	}

	@Test
	void testCustomFilterOrder() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.withPropertyValues("spring.security.filter.order:12345")
			.run((context) -> assertThat(
					context.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class)
						.getOrder())
				.isEqualTo(12345));
	}

	@Test
	void defaultFilterDispatcherTypes() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> {
				DelegatingFilterProxyRegistrationBean bean = context.getBean("securityFilterChainRegistration",
						DelegatingFilterProxyRegistrationBean.class);
				assertThat(bean).extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.containsExactlyInAnyOrderElementsOf(EnumSet.allOf(DispatcherType.class));
			});
	}

	@Test
	void customFilterDispatcherTypes() {
		this.contextRunner.withPropertyValues("spring.security.filter.dispatcher-types:INCLUDE,ERROR")
			.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> {
				DelegatingFilterProxyRegistrationBean bean = context.getBean("securityFilterChainRegistration",
						DelegatingFilterProxyRegistrationBean.class);
				assertThat(bean).extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.containsOnly(DispatcherType.INCLUDE, DispatcherType.ERROR);
			});
	}

	@Test
	void emptyFilterDispatcherTypesDoNotThrowException() {
		this.contextRunner.withPropertyValues("spring.security.filter.dispatcher-types:")
			.withConfiguration(AutoConfigurations.of(SecurityFilterAutoConfiguration.class))
			.run((context) -> {
				DelegatingFilterProxyRegistrationBean bean = context.getBean("securityFilterChainRegistration",
						DelegatingFilterProxyRegistrationBean.class);
				assertThat(bean).extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.isEmpty();
			});
	}

	@Test
	@WithPublicKeyResource
	void whenAConfigurationPropertyBindingConverterIsDefinedThenBindingToAnRsaKeySucceeds() {
		this.contextRunner.withUserConfiguration(ConverterConfiguration.class, PropertiesConfiguration.class)
			.withPropertyValues("jwt.public-key=classpath:public-key-location")
			.run((context) -> assertThat(context.getBean(JwtProperties.class).getPublicKey()).isNotNull());
	}

	@Test
	@WithPublicKeyResource
	void whenTheBeanFactoryHasAConversionServiceAndAConfigurationPropertyBindingConverterIsDefinedThenBindingToAnRsaKeySucceeds() {
		this.contextRunner
			.withInitializer(
					(context) -> context.getBeanFactory().setConversionService(new ApplicationConversionService()))
			.withUserConfiguration(ConverterConfiguration.class, PropertiesConfiguration.class)
			.withPropertyValues("jwt.public-key=classpath:public-key-location")
			.run((context) -> assertThat(context.getBean(JwtProperties.class).getPublicKey()).isNotNull());
	}

	@Test
	void whenDispatcherServletPathIsSetPathPatternRequestMatcherBuilderHasCustomBasePath() {
		this.contextRunner.withBean(DispatcherServletPath.class, () -> () -> "/dispatcher-servlet").run((context) -> {
			PathPatternRequestMatcher.Builder builder = context.getBean(PathPatternRequestMatcher.Builder.class);
			assertThat(builder).extracting("basePath").isEqualTo("/dispatcher-servlet");
		});
	}

	@Test
	void givenACustomPathPatternRequestMatcherBuilderWhenDispatcherServletPathIsSetBuilderBasePathIsNotCustomized() {
		this.contextRunner.withBean(PathPatternRequestMatcherBuilderFactoryBean.class)
			.withBean(DispatcherServletPath.class, () -> () -> "/dispatcher-servlet")
			.run((context) -> {
				PathPatternRequestMatcher.Builder builder = context.getBean(PathPatternRequestMatcher.Builder.class);
				assertThat(builder).extracting("basePath").isEqualTo("");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestSecurityFilterChainConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
			return http.securityMatcher("/**")
				.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
				.build();

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		static Converter<String, TargetType> targetTypeConverter() {
			return new Converter<>() {

				@Override
				public TargetType convert(String input) {
					return new TargetType();
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JwtProperties.class)
	static class PropertiesConfiguration {

	}

	@ConfigurationProperties("jwt")
	static class JwtProperties {

		private @Nullable RSAPublicKey publicKey;

		@Nullable RSAPublicKey getPublicKey() {
			return this.publicKey;
		}

		void setPublicKey(@Nullable RSAPublicKey publicKey) {
			this.publicKey = publicKey;
		}

	}

	static class TargetType {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "public-key-location", content = """
			-----BEGIN PUBLIC KEY-----
			MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDdlatRjRjogo3WojgGHFHYLugd
			UWAY9iR3fy4arWNA1KoS8kVw33cJibXr8bvwUAUparCwlvdbH6dvEOfou0/gCFQs
			HUfQrSDv+MuSUMAe8jzKE4qW+jK+xQU9a03GUnKHkkle+Q0pX/g6jXZ7r1/xAK5D
			o2kQ+X5xK9cipRgEKwIDAQAB
			-----END PUBLIC KEY-----
			""")
	@interface WithPublicKeyResource {

	}

}
