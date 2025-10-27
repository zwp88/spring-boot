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

package org.springframework.boot.webmvc.autoconfigure.error;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.servlet.ServletException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.util.ApplicationContextTestUtils;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvcTester} but not
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension}.
 *
 * @author Dave Syer
 * @author Sebastien Deleuze
 */
class BasicErrorControllerDirectMockMvcTests {

	@SuppressWarnings("NullAway.Init")
	private ConfigurableWebApplicationContext wac;

	@SuppressWarnings("NullAway.Init")
	private MockMvcTester mvc;

	@AfterEach
	void close() {
		ApplicationContextTestUtils.closeAll(this.wac);
	}

	void setup(ConfigurableWebApplicationContext context) {
		this.wac = context;
		this.mvc = MockMvcTester.from(this.wac);
	}

	@Test
	void errorPageAvailableWithParentContext() {
		setup((ConfigurableWebApplicationContext) new SpringApplicationBuilder(ParentConfiguration.class)
			.child(ChildConfiguration.class)
			.run("--server.port=0"));
		assertThat(this.mvc.get().uri("/error").accept(MediaType.TEXT_HTML)).hasStatus5xxServerError()
			.bodyText()
			.contains("status=999");
	}

	@Test
	void errorPageAvailableWithMvcIncluded() {
		setup((ConfigurableWebApplicationContext) new SpringApplication(WebMvcIncludedConfiguration.class)
			.run("--server.port=0"));
		assertThat(this.mvc.get().uri("/error").accept(MediaType.TEXT_HTML)).hasStatus5xxServerError()
			.bodyText()
			.contains("status=999");
	}

	@Test
	void errorPageNotAvailableWithWhitelabelDisabled() {
		setup((ConfigurableWebApplicationContext) new SpringApplication(WebMvcIncludedConfiguration.class)
			.run("--server.port=0", "--server.error.whitelabel.enabled=false"));
		assertThat(this.mvc.get().uri("/error").accept(MediaType.TEXT_HTML)).hasFailed()
			.failure()
			.isInstanceOf(ServletException.class);
	}

	@Test
	void errorControllerWithAop() {
		setup((ConfigurableWebApplicationContext) new SpringApplication(WithAopConfiguration.class)
			.run("--server.port=0"));
		assertThat(this.mvc.get().uri("/error").accept(MediaType.TEXT_HTML)).hasStatus5xxServerError()
			.bodyText()
			.contains("status=999");
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ TomcatServletWebServerAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	static class ParentConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	@EnableWebMvc
	static class WebMvcIncludedConfiguration {

		// For manual testing
		static void main(String[] args) {
			SpringApplication.run(WebMvcIncludedConfiguration.class, args);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	static class VanillaConfiguration {

		// For manual testing
		static void main(String[] args) {
			SpringApplication.run(VanillaConfiguration.class, args);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	static class ChildConfiguration {

		// For manual testing
		static void main(String[] args) {
			new SpringApplicationBuilder(ParentConfiguration.class).child(ChildConfiguration.class).run(args);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAspectJAutoProxy(proxyTargetClass = false)
	@MinimalWebConfiguration
	@Aspect
	static class WithAopConfiguration {

		@Pointcut("within(@org.springframework.stereotype.Controller *)")
		private void controllerPointCut() {
		}

		@Around("controllerPointCut()")
		Object mvcAdvice(ProceedingJoinPoint pjp) throws Throwable {
			return pjp.proceed();
		}

	}

}
