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

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.info.InfoPropertiesInfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.CloudFoundryWebEndpointDiscoverer;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet.CloudFoundryInfoEndpointWebExtension;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.MatcherSecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to expose actuator endpoints for
 * Cloud Foundry to use in a reactive environment.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(after = InfoEndpointAutoConfiguration.class,
		afterName = "org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration")
@ConditionalOnBooleanProperty(name = "management.cloudfoundry.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public final class CloudFoundryReactiveActuatorAutoConfiguration {

	private static final String BASE_PATH = "/cloudfoundryapplication";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnBean({ HealthEndpoint.class, ReactiveHealthEndpointWebExtension.class })
	CloudFoundryReactiveHealthEndpointWebExtension cloudFoundryReactiveHealthEndpointWebExtension(
			ReactiveHealthEndpointWebExtension reactiveHealthEndpointWebExtension) {
		return new CloudFoundryReactiveHealthEndpointWebExtension(reactiveHealthEndpointWebExtension);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	@ConditionalOnBean({ InfoEndpoint.class, GitProperties.class })
	CloudFoundryInfoEndpointWebExtension cloudFoundryInfoEndpointWebExtension(GitProperties properties,
			ObjectProvider<InfoContributor> infoContributors) {
		List<InfoContributor> contributors = infoContributors.orderedStream()
			.map((infoContributor) -> (infoContributor instanceof GitInfoContributor)
					? new GitInfoContributor(properties, InfoPropertiesInfoContributor.Mode.FULL) : infoContributor)
			.toList();
		return new CloudFoundryInfoEndpointWebExtension(new InfoEndpoint(contributors));
	}

	@Bean
	@SuppressWarnings("removal")
	CloudFoundryWebFluxEndpointHandlerMapping cloudFoundryWebFluxEndpointHandlerMapping(
			ParameterValueMapper parameterMapper, EndpointMediaTypes endpointMediaTypes,
			WebClient.Builder webClientBuilder,
			org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier controllerEndpointsSupplier,
			ApplicationContext applicationContext) {
		CloudFoundryWebEndpointDiscoverer endpointDiscoverer = new CloudFoundryWebEndpointDiscoverer(applicationContext,
				parameterMapper, endpointMediaTypes, null, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList());
		SecurityInterceptor securityInterceptor = getSecurityInterceptor(webClientBuilder,
				applicationContext.getEnvironment());
		Collection<ExposableWebEndpoint> webEndpoints = endpointDiscoverer.getEndpoints();
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		return new CloudFoundryWebFluxEndpointHandlerMapping(new EndpointMapping(BASE_PATH), webEndpoints,
				endpointMediaTypes, getCorsConfiguration(), securityInterceptor, allEndpoints);
	}

	private SecurityInterceptor getSecurityInterceptor(WebClient.Builder webClientBuilder, Environment environment) {
		SecurityService cloudfoundrySecurityService = getCloudFoundrySecurityService(webClientBuilder, environment);
		TokenValidator tokenValidator = (cloudfoundrySecurityService != null)
				? new TokenValidator(cloudfoundrySecurityService) : null;
		return new SecurityInterceptor(tokenValidator, cloudfoundrySecurityService,
				environment.getProperty("vcap.application.application_id"));
	}

	private @Nullable SecurityService getCloudFoundrySecurityService(WebClient.Builder webClientBuilder,
			Environment environment) {
		String cloudControllerUrl = environment.getProperty("vcap.application.cf_api");
		boolean skipSslValidation = environment.getProperty("management.cloudfoundry.skip-ssl-validation",
				Boolean.class, false);
		return (cloudControllerUrl != null)
				? new SecurityService(webClientBuilder, cloudControllerUrl, skipSslValidation) : null;
	}

	private CorsConfiguration getCorsConfiguration() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);
		corsConfiguration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
		corsConfiguration
			.setAllowedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION, "X-Cf-App-Instance", HttpHeaders.CONTENT_TYPE));
		return corsConfiguration;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MatcherSecurityWebFilterChain.class)
	static class IgnoredPathsSecurityConfiguration {

		@Bean
		static WebFilterChainPostProcessor webFilterChainPostProcessor(
				ObjectProvider<CloudFoundryWebFluxEndpointHandlerMapping> handlerMapping) {
			return new WebFilterChainPostProcessor(handlerMapping);
		}

	}

	static class WebFilterChainPostProcessor implements BeanPostProcessor {

		private final Supplier<PathMappedEndpoints> pathMappedEndpoints;

		WebFilterChainPostProcessor(ObjectProvider<CloudFoundryWebFluxEndpointHandlerMapping> handlerMapping) {
			this.pathMappedEndpoints = SingletonSupplier
				.of(() -> new PathMappedEndpoints(BASE_PATH, () -> handlerMapping.getObject().getAllEndpoints()));
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof WebFilterChainProxy webFilterChainProxy) {
				return postProcess(webFilterChainProxy);
			}
			return bean;
		}

		private WebFilterChainProxy postProcess(WebFilterChainProxy existing) {
			List<String> paths = getPaths(this.pathMappedEndpoints.get());
			ServerWebExchangeMatcher cloudFoundryRequestMatcher = ServerWebExchangeMatchers
				.pathMatchers(paths.toArray(new String[] {}));
			WebFilter noOpFilter = (exchange, chain) -> chain.filter(exchange);
			MatcherSecurityWebFilterChain ignoredRequestFilterChain = new MatcherSecurityWebFilterChain(
					cloudFoundryRequestMatcher, Collections.singletonList(noOpFilter));
			MatcherSecurityWebFilterChain allRequestsFilterChain = new MatcherSecurityWebFilterChain(
					ServerWebExchangeMatchers.anyExchange(), Collections.singletonList(existing));
			return new WebFilterChainProxy(ignoredRequestFilterChain, allRequestsFilterChain);
		}

		private static List<String> getPaths(PathMappedEndpoints pathMappedEndpoints) {
			List<String> paths = new ArrayList<>();
			pathMappedEndpoints.getAllPaths().forEach((path) -> paths.add(path + "/**"));
			paths.add(BASE_PATH);
			paths.add(BASE_PATH + "/");
			return paths;
		}

	}

}
