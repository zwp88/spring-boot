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

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Tests for {@link ReactiveManagementWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class ReactiveManagementWebSecurityAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
				InfoEndpointAutoConfiguration.class, WebFluxAutoConfiguration.class,
				EnvironmentEndpointAutoConfiguration.class, EndpointAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, ReactiveWebSecurityAutoConfiguration.class,
				ReactiveManagementWebSecurityAutoConfiguration.class));

	@Test
	void permitAllForHealth() {
		this.contextRunner.withUserConfiguration(UserDetailsServiceConfiguration.class)
			.run((context) -> assertThat(getAuthenticateHeader(context, "/actuator/health")).isNull());
	}

	@Test
	void withAdditionalPathsOnSamePort() {
		this.contextRunner.withUserConfiguration(UserDetailsServiceConfiguration.class)
			.withPropertyValues("management.endpoint.health.group.test1.include=*",
					"management.endpoint.health.group.test2.include=*",
					"management.endpoint.health.group.test1.additional-path=server:/check1",
					"management.endpoint.health.group.test2.additional-path=management:/check2")
			.run((context) -> {
				assertThat(getAuthenticateHeader(context, "/check1")).isNull();
				assertThat(getRequiredAuthenticateHeader(context, "/check2").get(0)).contains("Basic realm=");
				assertThat(getAuthenticateHeader(context, "/actuator/health")).isNull();
			});
	}

	@Test
	void withAdditionalPathsOnDifferentPort() {
		this.contextRunner.withUserConfiguration(UserDetailsServiceConfiguration.class)
			.withPropertyValues("management.endpoint.health.group.test1.include=*",
					"management.endpoint.health.group.test2.include=*",
					"management.endpoint.health.group.test1.additional-path=server:/check1",
					"management.endpoint.health.group.test2.additional-path=management:/check2",
					"management.server.port=0")
			.run((context) -> {
				assertThat(getAuthenticateHeader(context, "/check1")).isNull();
				assertThat(getRequiredAuthenticateHeader(context, "/check2").get(0)).contains("Basic realm=");
				assertThat(getRequiredAuthenticateHeader(context, "/actuator/health").get(0)).contains("Basic realm=");
			});
	}

	@Test
	void securesEverythingElse() {
		this.contextRunner.withUserConfiguration(UserDetailsServiceConfiguration.class).run((context) -> {
			assertThat(getRequiredAuthenticateHeader(context, "/actuator").get(0)).contains("Basic realm=");
			assertThat(getRequiredAuthenticateHeader(context, "/foo").toString()).contains("Basic realm=");
		});
	}

	@Test
	void noExistingAuthenticationManagerOrUserDetailsService() {
		this.contextRunner.run((context) -> {
			assertThat(getAuthenticateHeader(context, "/actuator/health")).isNull();
			assertThat(getRequiredAuthenticateHeader(context, "/actuator").get(0)).contains("Basic realm=");
			assertThat(getRequiredAuthenticateHeader(context, "/foo").toString()).contains("Basic realm=");
		});
	}

	@Test
	void usesMatchersBasedOffConfiguredActuatorBasePath() {
		this.contextRunner.withUserConfiguration(UserDetailsServiceConfiguration.class)
			.withPropertyValues("management.endpoints.web.base-path=/")
			.run((context) -> {
				assertThat(getAuthenticateHeader(context, "/health")).isNull();
				assertThat(getRequiredAuthenticateHeader(context, "/foo").get(0)).contains("Basic realm=");
			});
	}

	@Test
	void backsOffIfCustomSecurityIsAdded() {
		this.contextRunner.withUserConfiguration(CustomSecurityConfiguration.class).run((context) -> {
			assertThat(getRequiredLocationHeader(context, "/actuator/health").toString()).contains("/login");
			assertThat(getLocationHeader(context, "/foo")).isNull();
		});
	}

	@Test
	void backsOffWhenWebFilterChainProxyBeanPresent() {
		this.contextRunner.withUserConfiguration(WebFilterChainProxyConfiguration.class).run((context) -> {
			assertThat(getRequiredLocationHeader(context, "/actuator/health").toString()).contains("/login");
			assertThat(getRequiredLocationHeader(context, "/foo").toString()).contains("/login");
		});
	}

	private @Nullable List<String> getAuthenticateHeader(AssertableReactiveWebApplicationContext context, String path) {
		ServerWebExchange exchange = performFilter(context, path);
		return exchange.getResponse().getHeaders().get(HttpHeaders.WWW_AUTHENTICATE);
	}

	private List<String> getRequiredAuthenticateHeader(AssertableReactiveWebApplicationContext context, String path) {
		List<String> header = getAuthenticateHeader(context, path);
		assertThat(header).isNotNull();
		return header;
	}

	private ServerWebExchange performFilter(AssertableReactiveWebApplicationContext context, String path) {
		ServerWebExchange exchange = webHandler(context).createExchange(MockServerHttpRequest.get(path).build(),
				new MockServerHttpResponse());
		WebFilterChainProxy proxy = context.getBean(WebFilterChainProxy.class);
		proxy.filter(exchange, (serverWebExchange) -> Mono.empty()).block(Duration.ofSeconds(30));
		return exchange;
	}

	private @Nullable URI getLocationHeader(AssertableReactiveWebApplicationContext context, String path) {
		ServerWebExchange exchange = performFilter(context, path);
		return exchange.getResponse().getHeaders().getLocation();
	}

	private URI getRequiredLocationHeader(AssertableReactiveWebApplicationContext context, String path) {
		URI header = getLocationHeader(context, path);
		assertThat(header).isNotNull();
		return header;
	}

	private TestHttpWebHandlerAdapter webHandler(AssertableReactiveWebApplicationContext context) {
		TestHttpWebHandlerAdapter adapter = new TestHttpWebHandlerAdapter(mock(WebHandler.class));
		adapter.setApplicationContext(context);
		return adapter;
	}

	static class TestHttpWebHandlerAdapter extends HttpWebHandlerAdapter {

		TestHttpWebHandlerAdapter(WebHandler delegate) {
			super(delegate);
		}

		@Override
		protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
			return super.createExchange(request, response);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserDetailsServiceConfiguration {

		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return new MapReactiveUserDetailsService(
					User.withUsername("alice").password("secret").roles("admin").build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSecurityConfiguration {

		@Bean
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			http.authorizeExchange((exchanges) -> {
				exchanges.pathMatchers("/foo").permitAll();
				exchanges.anyExchange().authenticated();
			});
			http.formLogin(withDefaults());
			return http.build();
		}

		@Bean
		ReactiveAuthenticationManager authenticationManager() {
			return mock(ReactiveAuthenticationManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WebFilterChainProxyConfiguration {

		@Bean
		ReactiveAuthenticationManager authenticationManager() {
			return mock(ReactiveAuthenticationManager.class);
		}

		@Bean
		WebFilterChainProxy webFilterChainProxy(ServerHttpSecurity http) {
			return new WebFilterChainProxy(getFilterChains(http));
		}

		@Bean
		TestServerHttpSecurity http(ReactiveAuthenticationManager authenticationManager) {
			TestServerHttpSecurity httpSecurity = new TestServerHttpSecurity();
			httpSecurity.authenticationManager(authenticationManager);
			return httpSecurity;
		}

		private List<SecurityWebFilterChain> getFilterChains(ServerHttpSecurity http) {
			http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated());
			http.formLogin(withDefaults());
			return Collections.singletonList(http.build());
		}

		static class TestServerHttpSecurity extends ServerHttpSecurity implements ApplicationContextAware {

			@Override
			public void setApplicationContext(ApplicationContext applicationContext) {
				super.setApplicationContext(applicationContext);
			}

		}

	}

}
