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

package org.springframework.boot.security.autoconfigure.actuate.web.servlet;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Tests for {@link ManagementWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Hatef Palizgar
 */
class ManagementWebSecurityAutoConfigurationTests {

	private static final String MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN = "managementSecurityFilterChain";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(contextSupplier(),
			WebServerApplicationContext.class)
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				InfoEndpointAutoConfiguration.class, EnvironmentEndpointAutoConfiguration.class,
				EndpointAutoConfiguration.class, WebMvcAutoConfiguration.class, WebEndpointAutoConfiguration.class,
				SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class));

	private static Supplier<ConfigurableWebApplicationContext> contextSupplier() {
		return WebApplicationContextRunner.withMockServletContext(MockWebServerApplicationContext::new);
	}

	@Test
	void permitAllForHealth() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasBean(MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN);
			HttpStatus status = getResponseStatus(context, "/actuator/health");
			assertThat(status).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void securesEverythingElse() {
		this.contextRunner.run((context) -> {
			HttpStatus status = getResponseStatus(context, "/actuator");
			assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
			status = getResponseStatus(context, "/foo");
			assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
		});
	}

	@Test
	void autoConfigIsConditionalOnSecurityFilterChainClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SecurityFilterChain.class)).run((context) -> {
			assertThat(context).doesNotHaveBean(ManagementWebSecurityAutoConfiguration.class);
			HttpStatus status = getResponseStatus(context, "/actuator/health");
			assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
		});
	}

	@Test
	void usesMatchersBasedOffConfiguredActuatorBasePath() {
		this.contextRunner.withPropertyValues("management.endpoints.web.base-path=/").run((context) -> {
			HttpStatus status = getResponseStatus(context, "/health");
			assertThat(status).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void backOffIfCustomSecurityIsAdded() {
		this.contextRunner.withUserConfiguration(CustomSecurityConfiguration.class).run((context) -> {
			HttpStatus status = getResponseStatus(context, "/actuator/health");
			assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
			status = getResponseStatus(context, "/foo");
			assertThat(status).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void backsOffIfSecurityFilterChainBeanIsPresent() {
		this.contextRunner.withUserConfiguration(TestSecurityFilterChainConfig.class).run((context) -> {
			assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(1);
			assertThat(context.containsBean("testSecurityFilterChain")).isTrue();
		});
	}

	@Test
	void backOffIfRemoteDevToolsSecurityFilterChainIsPresent() {
		this.contextRunner.withUserConfiguration(TestRemoteDevToolsSecurityFilterChainConfig.class).run((context) -> {
			SecurityFilterChain testSecurityFilterChain = context.getBean("testSecurityFilterChain",
					SecurityFilterChain.class);
			SecurityFilterChain testRemoteDevToolsSecurityFilterChain = context
				.getBean("testRemoteDevToolsSecurityFilterChain", SecurityFilterChain.class);
			List<SecurityFilterChain> orderedSecurityFilterChains = context.getBeanProvider(SecurityFilterChain.class)
				.orderedStream()
				.toList();
			assertThat(orderedSecurityFilterChains).containsExactly(testRemoteDevToolsSecurityFilterChain,
					testSecurityFilterChain);
			assertThat(context).doesNotHaveBean(ManagementWebSecurityAutoConfiguration.class);
		});
	}

	@Test
	void withAdditionalPathsOnSamePort() {
		this.contextRunner
			.withPropertyValues("management.endpoint.health.group.test1.include=*",
					"management.endpoint.health.group.test2.include=*",
					"management.endpoint.health.group.test1.additional-path=server:/check1",
					"management.endpoint.health.group.test2.additional-path=management:/check2")
			.run((context) -> {
				assertThat(getResponseStatus(context, "/check1")).isEqualTo(HttpStatus.OK);
				assertThat(getResponseStatus(context, "/check2")).isEqualTo(HttpStatus.UNAUTHORIZED);
				assertThat(getResponseStatus(context, "/actuator/health")).isEqualTo(HttpStatus.OK);
			});
	}

	@Test
	void withAdditionalPathsOnDifferentPort() {
		this.contextRunner.withPropertyValues("management.endpoint.health.group.test1.include=*",
				"management.endpoint.health.group.test2.include=*",
				"management.endpoint.health.group.test1.additional-path=server:/check1",
				"management.endpoint.health.group.test2.additional-path=management:/check2", "management.server.port=0")
			.run((context) -> {
				assertThat(getResponseStatus(context, "/check1")).isEqualTo(HttpStatus.OK);
				assertThat(getResponseStatus(context, "/check2")).isEqualTo(HttpStatus.UNAUTHORIZED);
				assertThat(getResponseStatus(context, "/actuator/health")).isEqualTo(HttpStatus.UNAUTHORIZED);
			});
	}

	private HttpStatus getResponseStatus(AssertableWebApplicationContext context, String path)
			throws IOException, jakarta.servlet.ServletException {
		FilterChainProxy filterChainProxy = context.getBean(FilterChainProxy.class);
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletResponse response = new MockHttpServletResponse();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setRequestURI(path);
		request.setMethod("GET");
		filterChainProxy.doFilter(request, response, new MockFilterChain());
		return HttpStatus.valueOf(response.getStatus());
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSecurityConfiguration {

		@Bean
		SecurityFilterChain securityFilterChain(HttpSecurity http) {
			http.authorizeHttpRequests((requests) -> {
				requests.requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/foo")).permitAll();
				requests.anyRequest().authenticated();
			});
			http.formLogin(withDefaults());
			http.httpBasic(withDefaults());
			return http.build();
		}

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
	static class TestRemoteDevToolsSecurityFilterChainConfig extends TestSecurityFilterChainConfig {

		@Bean
		@Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
		SecurityFilterChain testRemoteDevToolsSecurityFilterChain(HttpSecurity http) {
			http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/**"));
			http.authorizeHttpRequests((requests) -> requests.anyRequest().anonymous());
			http.csrf((csrf) -> csrf.disable());
			return http.build();
		}

	}

	static class MockWebServerApplicationContext extends AnnotationConfigServletWebApplicationContext
			implements WebServerApplicationContext {

		@Override
		public @Nullable WebServer getWebServer() {
			return null;
		}

		@Override
		public String getServerNamespace() {
			return "server";
		}

	}

}
