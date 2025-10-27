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

package org.springframework.boot.security.autoconfigure.actuate.web.reactive;

import java.time.Duration;
import java.util.Base64;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link EndpointRequest}.
 *
 * @author Chris Bono
 */
class EndpointRequestIntegrationTests {

	@Test
	void toEndpointShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e1").exchange().expectStatus().isOk();
		});
	}

	@Test
	void toEndpointPostShouldMatch() {
		getContextRunner().withPropertyValues("spring.security.user.password=password").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.post().uri("/actuator/e1").exchange().expectStatus().isUnauthorized();
			webTestClient.post()
				.uri("/actuator/e1")
				.header("Authorization", getBasicAuth())
				.exchange()
				.expectStatus()
				.isNoContent();
		});
	}

	@Test
	void toAllEndpointsShouldMatch() {
		getContextRunner().withPropertyValues("spring.security.user.password=password").run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator/e2").exchange().expectStatus().isUnauthorized();
			webTestClient.get()
				.uri("/actuator/e2")
				.header("Authorization", getBasicAuth())
				.exchange()
				.expectStatus()
				.isOk();
		});
	}

	@Test
	void toLinksShouldMatch() {
		getContextRunner().run((context) -> {
			WebTestClient webTestClient = getWebTestClient(context);
			webTestClient.get().uri("/actuator").exchange().expectStatus().isOk();
		});
	}

	protected final ReactiveWebApplicationContextRunner getContextRunner() {
		return createContextRunner().withPropertyValues("management.endpoints.web.exposure.include=*")
			.withUserConfiguration(BaseConfiguration.class, SecurityConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(JacksonAutoConfiguration.class, ReactiveWebSecurityAutoConfiguration.class,
							ReactiveUserDetailsServiceAutoConfiguration.class, EndpointAutoConfiguration.class,
							WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class));

	}

	protected ReactiveWebApplicationContextRunner createContextRunner() {
		return new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
			.withUserConfiguration(WebEndpointConfiguration.class)
			.withConfiguration(AutoConfigurations.of(HttpHandlerAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, WebFluxAutoConfiguration.class));
	}

	protected WebTestClient getWebTestClient(AssertableReactiveWebApplicationContext context) {
		WebServer webServer = context
			.getSourceApplicationContext(AnnotationConfigReactiveWebServerApplicationContext.class)
			.getWebServer();
		assertThat(webServer).isNotNull();
		int port = webServer.getPort();
		return WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + port)
			.responseTimeout(Duration.ofMinutes(5))
			.build();
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		TestEndpoint1 endpoint1() {
			return new TestEndpoint1();
		}

		@Bean
		TestEndpoint2 endpoint2() {
			return new TestEndpoint2();
		}

		@Bean
		TestEndpoint3 endpoint3() {
			return new TestEndpoint3();
		}

	}

	@Endpoint(id = "e1")
	static class TestEndpoint1 {

		@ReadOperation
		Object getAll() {
			return "endpoint 1";
		}

		@WriteOperation
		void setAll() {
		}

	}

	@Endpoint(id = "e2")
	static class TestEndpoint2 {

		@ReadOperation
		Object getAll() {
			return "endpoint 2";
		}

	}

	@Endpoint(id = "e3")
	static class TestEndpoint3 {

		@ReadOperation
		@Nullable Object getAll() {
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WebEndpointProperties.class)
	static class WebEndpointConfiguration {

		@Bean
		TomcatReactiveWebServerFactory tomcat() {
			return new TomcatReactiveWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@SuppressWarnings("deprecation")
		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return new MapReactiveUserDetailsService(
					User.withDefaultPasswordEncoder()
						.username("user")
						.password("password")
						.authorities("ROLE_USER")
						.build(),
					User.withDefaultPasswordEncoder()
						.username("admin")
						.password("admin")
						.authorities("ROLE_ACTUATOR", "ROLE_USER")
						.build());
		}

		@Bean
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			http.authorizeExchange((exchanges) -> {
				exchanges.matchers(EndpointRequest.toLinks()).permitAll();
				exchanges.matchers(EndpointRequest.to(TestEndpoint1.class).withHttpMethod(HttpMethod.POST))
					.authenticated();
				exchanges.matchers(EndpointRequest.to(TestEndpoint1.class)).permitAll();
				exchanges.matchers(EndpointRequest.toAnyEndpoint()).authenticated();
				exchanges.anyExchange().hasRole("ADMIN");
			});
			http.httpBasic(Customizer.withDefaults());
			http.csrf(CsrfSpec::disable);
			return http.build();
		}

	}

}
