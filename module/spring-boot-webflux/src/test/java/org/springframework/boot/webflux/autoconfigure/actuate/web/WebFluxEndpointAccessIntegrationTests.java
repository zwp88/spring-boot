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

package org.springframework.boot.webflux.autoconfigure.actuate.web;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.reactor.netty.autoconfigure.NettyReactiveWebServerAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for controlling access to endpoints exposed by Spring WebFlux.
 *
 * @author Andy Wilkinson
 */
class WebFluxEndpointAccessIntegrationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner(
			AnnotationConfigReactiveWebServerApplicationContext::new)
		.withConfiguration(AutoConfigurations.of(NettyReactiveWebServerAutoConfiguration.class,
				HttpHandlerAutoConfiguration.class, JacksonAutoConfiguration.class, CodecsAutoConfiguration.class,
				WebFluxAutoConfiguration.class, EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
				ManagementContextAutoConfiguration.class))
		.withConfiguration(AutoConfigurations.of(BeansEndpointAutoConfiguration.class))
		.withUserConfiguration(CustomWebFluxEndpoint.class)
		.withPropertyValues("server.port:0");

	@Test
	void accessIsUnrestrictedByDefault() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=*").run((context) -> {
			WebTestClient client = createClient(context);
			assertThat(isAccessible(client, HttpMethod.GET, "beans")).isTrue();
			assertThat(isAccessible(client, HttpMethod.GET, "customwebflux")).isTrue();
			assertThat(isAccessible(client, HttpMethod.POST, "customwebflux")).isTrue();
		});
	}

	@Test
	void accessCanBeReadOnlyByDefault() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoints.access.default=READ_ONLY")
			.run((context) -> {
				WebTestClient client = createClient(context);
				assertThat(isAccessible(client, HttpMethod.GET, "beans")).isTrue();
				assertThat(isAccessible(client, HttpMethod.GET, "customwebflux")).isTrue();
				assertThat(isAccessible(client, HttpMethod.POST, "customwebflux")).isFalse();
			});
	}

	@Test
	void accessCanBeNoneByDefault() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoints.access.default=NONE")
			.run((context) -> {
				WebTestClient client = createClient(context);
				assertThat(isAccessible(client, HttpMethod.GET, "beans")).isFalse();
				assertThat(isAccessible(client, HttpMethod.GET, "customwebflux")).isFalse();
				assertThat(isAccessible(client, HttpMethod.POST, "customwebflux")).isFalse();
			});
	}

	@Test
	void accessForOneEndpointCanOverrideTheDefaultAccess() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoints.access.default=NONE", "management.endpoint.customwebflux.access=UNRESTRICTED")
			.run((context) -> {
				WebTestClient client = createClient(context);
				assertThat(isAccessible(client, HttpMethod.GET, "beans")).isFalse();
				assertThat(isAccessible(client, HttpMethod.GET, "customwebflux")).isTrue();
				assertThat(isAccessible(client, HttpMethod.POST, "customwebflux")).isTrue();
			});
	}

	@Test
	void accessCanBeCappedAtReadOnly() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoints.access.default=UNRESTRICTED",
					"management.endpoints.access.max-permitted=READ_ONLY")
			.run((context) -> {
				WebTestClient client = createClient(context);
				assertThat(isAccessible(client, HttpMethod.GET, "beans")).isTrue();
				assertThat(isAccessible(client, HttpMethod.GET, "customwebflux")).isTrue();
				assertThat(isAccessible(client, HttpMethod.POST, "customwebflux")).isFalse();
			});
	}

	@Test
	void accessCanBeCappedAtNone() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=*",
				"management.endpoints.access.default=UNRESTRICTED", "management.endpoints.access.max-permitted=NONE")
			.run((context) -> {
				WebTestClient client = createClient(context);
				assertThat(isAccessible(client, HttpMethod.GET, "beans")).isFalse();
				assertThat(isAccessible(client, HttpMethod.GET, "customwebflux")).isFalse();
				assertThat(isAccessible(client, HttpMethod.POST, "customwebflux")).isFalse();
			});
	}

	private WebTestClient createClient(AssertableReactiveWebApplicationContext context) {
		WebServer webServer = context.getSourceApplicationContext(ReactiveWebServerApplicationContext.class)
			.getWebServer();
		assertThat(webServer).isNotNull();
		int port = webServer.getPort();
		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
			.codecs((configurer) -> configurer.defaultCodecs().maxInMemorySize(-1))
			.build();
		return WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + port)
			.exchangeStrategies(exchangeStrategies)
			.responseTimeout(Duration.ofMinutes(5))
			.build();
	}

	private boolean isAccessible(WebTestClient client, HttpMethod method, String path) {
		path = "/actuator/" + path;
		EntityExchangeResult<byte[]> result = client.method(method).uri(path).exchange().expectBody().returnResult();
		if (result.getStatus() == HttpStatus.OK) {
			return true;
		}
		if (result.getStatus() == HttpStatus.NOT_FOUND || result.getStatus() == HttpStatus.METHOD_NOT_ALLOWED) {
			return false;
		}
		throw new IllegalStateException(
				String.format("Unexpected %s HTTP status for endpoint %s", result.getStatus(), path));
	}

	@org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint(id = "customwebflux")
	@SuppressWarnings("removal")
	static class CustomWebFluxEndpoint {

		@GetMapping("/")
		String get() {
			return "get";
		}

		@PostMapping("/")
		String post() {
			return "post";
		}

	}

}
