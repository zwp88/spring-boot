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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.AdditionalPathsMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for web {@link Endpoint @Endpoint}
 * support.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Yongjun Hong
 * @since 2.0.0
 */
@AutoConfiguration(after = EndpointAutoConfiguration.class)
@ConditionalOnWebApplication
@EnableConfigurationProperties(WebEndpointProperties.class)
public final class WebEndpointAutoConfiguration {

	private final ApplicationContext applicationContext;

	private final WebEndpointProperties properties;

	WebEndpointAutoConfiguration(ApplicationContext applicationContext, WebEndpointProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@Bean
	PathMapper webEndpointPathMapper() {
		return new MappingWebEndpointPathMapper(this.properties.getPathMapping());
	}

	@Bean
	@ConditionalOnMissingBean
	EndpointMediaTypes endpointMediaTypes() {
		return EndpointMediaTypes.DEFAULT;
	}

	@Bean
	@ConditionalOnMissingBean(WebEndpointsSupplier.class)
	WebEndpointDiscoverer webEndpointDiscoverer(ParameterValueMapper parameterValueMapper,
			EndpointMediaTypes endpointMediaTypes, ObjectProvider<PathMapper> endpointPathMappers,
			ObjectProvider<AdditionalPathsMapper> additionalPathsMappers,
			ObjectProvider<OperationInvokerAdvisor> invokerAdvisors,
			ObjectProvider<EndpointFilter<ExposableWebEndpoint>> endpointFilters,
			ObjectProvider<OperationFilter<WebOperation>> operationFilters) {
		return new WebEndpointDiscoverer(this.applicationContext, parameterValueMapper, endpointMediaTypes,
				endpointPathMappers.orderedStream().toList(), additionalPathsMappers.orderedStream().toList(),
				invokerAdvisors.orderedStream().toList(), endpointFilters.orderedStream().toList(),
				operationFilters.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean(org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier.class)
	@SuppressWarnings({ "deprecation", "removal" })
	org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointDiscoverer controllerEndpointDiscoverer(
			ObjectProvider<PathMapper> endpointPathMappers,
			ObjectProvider<Collection<EndpointFilter<org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint>>> filters) {
		return new org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointDiscoverer(
				this.applicationContext, endpointPathMappers.orderedStream().toList(),
				filters.getIfAvailable(Collections::emptyList));
	}

	@Bean
	@ConditionalOnMissingBean
	PathMappedEndpoints pathMappedEndpoints(Collection<EndpointsSupplier<?>> endpointSuppliers) {
		String basePath = this.properties.getBasePath();
		PathMappedEndpoints pathMappedEndpoints = new PathMappedEndpoints(basePath, endpointSuppliers);
		if ((!StringUtils.hasText(basePath) || "/".equals(basePath))
				&& ManagementPortType.get(this.applicationContext.getEnvironment()) == ManagementPortType.SAME) {
			assertHasNoRootPaths(pathMappedEndpoints);
		}
		return pathMappedEndpoints;
	}

	private void assertHasNoRootPaths(PathMappedEndpoints endpoints) {
		for (PathMappedEndpoint endpoint : endpoints) {
			if (endpoint instanceof ExposableWebEndpoint webEndpoint) {
				Assert.state(!isMappedToRootPath(webEndpoint),
						() -> "Management base path and the '" + webEndpoint.getEndpointId()
								+ "' actuator endpoint are both mapped to '/' "
								+ "on the server port which will block access to other endpoints. "
								+ "Please use a different path for management endpoints or map them to a "
								+ "dedicated management port.");
			}

		}
	}

	private boolean isMappedToRootPath(PathMappedEndpoint endpoint) {
		return endpoint.getRootPath().equals("/")
				|| endpoint.getAdditionalPaths(WebServerNamespace.SERVER).contains("/");
	}

	@Bean
	IncludeExcludeEndpointFilter<ExposableWebEndpoint> webExposeExcludePropertyEndpointFilter() {
		WebEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new IncludeExcludeEndpointFilter<>(ExposableWebEndpoint.class, exposure.getInclude(),
				exposure.getExclude(), EndpointExposure.WEB.getDefaultIncludes());
	}

	@Bean
	@SuppressWarnings("removal")
	IncludeExcludeEndpointFilter<org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint> controllerExposeExcludePropertyEndpointFilter() {
		WebEndpointProperties.Exposure exposure = this.properties.getExposure();
		return new IncludeExcludeEndpointFilter<>(
				org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint.class,
				exposure.getInclude(), exposure.getExclude());
	}

	@Bean
	OperationFilter<WebOperation> webAccessPropertiesOperationFilter(EndpointAccessResolver endpointAccessResolver) {
		return OperationFilter.byAccess(endpointAccessResolver);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class WebEndpointServletConfiguration {

		@Bean
		@SuppressWarnings({ "deprecation", "removal" })
		@ConditionalOnMissingBean(org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier.class)
		org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer servletEndpointDiscoverer(
				ApplicationContext applicationContext, ObjectProvider<PathMapper> endpointPathMappers,
				ObjectProvider<EndpointFilter<org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint>> filters) {
			return new org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer(
					applicationContext, endpointPathMappers.orderedStream().toList(), filters.orderedStream().toList());
		}

	}

}
