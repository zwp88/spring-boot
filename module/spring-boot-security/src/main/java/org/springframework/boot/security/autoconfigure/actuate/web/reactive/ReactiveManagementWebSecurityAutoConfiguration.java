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

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.reactive.PreFlightRequestHandler;
import org.springframework.web.cors.reactive.PreFlightRequestWebFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactive Spring Security when
 * actuator is on the classpath. Specifically, it permits access to the health endpoint
 * while securing everything else.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(before = ReactiveWebSecurityAutoConfiguration.class,
		afterName = { "org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration",
				"org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration",
				"org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration" })
@ConditionalOnClass({ EnableWebFluxSecurity.class, WebFilterChainProxy.class, WebEndpointAutoConfiguration.class })
@ConditionalOnMissingBean({ SecurityWebFilterChain.class, WebFilterChainProxy.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
public final class ReactiveManagementWebSecurityAutoConfiguration {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, PreFlightRequestHandler handler) {
		http.authorizeExchange((exchanges) -> {
			exchanges.matchers(healthMatcher(), additionalHealthPathsMatcher()).permitAll();
			exchanges.anyExchange().authenticated();
		});
		PreFlightRequestWebFilter filter = new PreFlightRequestWebFilter(handler);
		http.addFilterAt(filter, SecurityWebFiltersOrder.CORS);
		http.httpBasic(withDefaults());
		http.formLogin(withDefaults());
		return http.build();
	}

	private ServerWebExchangeMatcher healthMatcher() {
		return EndpointRequest.to(HealthEndpoint.class);
	}

	private ServerWebExchangeMatcher additionalHealthPathsMatcher() {
		return EndpointRequest.toAdditionalPaths(WebServerNamespace.SERVER, HealthEndpoint.class);
	}

	@Bean
	@ConditionalOnMissingBean({ ReactiveAuthenticationManager.class, ReactiveUserDetailsService.class })
	ReactiveAuthenticationManager denyAllAuthenticationManager() {
		return (authentication) -> Mono.error(new UsernameNotFoundException(authentication.getName()));
	}

}
