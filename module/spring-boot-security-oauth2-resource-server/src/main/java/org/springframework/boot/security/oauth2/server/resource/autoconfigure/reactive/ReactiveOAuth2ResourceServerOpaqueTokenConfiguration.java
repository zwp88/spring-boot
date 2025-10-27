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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.Assert;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Configures a {@link ReactiveOpaqueTokenIntrospector} when a token introspection
 * endpoint is available. Also configures a {@link SecurityWebFilterChain} if a
 * {@link ReactiveOpaqueTokenIntrospector} bean is found.
 *
 * @author Madhura Bhave
 */
class ReactiveOAuth2ResourceServerOpaqueTokenConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveOpaqueTokenIntrospector.class)
	static class OpaqueTokenIntrospectionClientConfiguration {

		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
		SpringReactiveOpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2ResourceServerProperties properties) {
			OAuth2ResourceServerProperties.Opaquetoken opaquetoken = properties.getOpaquetoken();
			String clientId = opaquetoken.getClientId();
			Assert.state(clientId != null, "'clientId' must not be null");
			String clientSecret = opaquetoken.getClientSecret();
			Assert.state(clientSecret != null, "'clientSecret' must not be null");
			return SpringReactiveOpaqueTokenIntrospector.withIntrospectionUri(opaquetoken.getIntrospectionUri())
				.clientId(clientId)
				.clientSecret(clientSecret)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SecurityWebFilterChain.class)
	static class WebSecurityConfiguration {

		@Bean
		@ConditionalOnBean(ReactiveOpaqueTokenIntrospector.class)
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated());
			http.oauth2ResourceServer((resourceServer) -> resourceServer.opaqueToken(withDefaults()));
			return http.build();
		}

	}

}
