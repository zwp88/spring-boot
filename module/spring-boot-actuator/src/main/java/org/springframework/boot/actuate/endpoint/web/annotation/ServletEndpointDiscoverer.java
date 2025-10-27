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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * {@link EndpointDiscoverer} for {@link ExposableServletEndpoint servlet endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint}
 */
@ImportRuntimeHints(ServletEndpointDiscoverer.ServletEndpointDiscovererRuntimeHints.class)
@Deprecated(since = "3.3.0", forRemoval = true)
@SuppressWarnings("removal")
public class ServletEndpointDiscoverer extends EndpointDiscoverer<ExposableServletEndpoint, Operation>
		implements ServletEndpointsSupplier {

	private final @Nullable List<PathMapper> endpointPathMappers;

	/**
	 * Create a new {@link ServletEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param endpointPathMappers the endpoint path mappers
	 * @param filters filters to apply
	 */
	public ServletEndpointDiscoverer(ApplicationContext applicationContext,
			@Nullable List<PathMapper> endpointPathMappers,
			Collection<EndpointFilter<ExposableServletEndpoint>> filters) {
		super(applicationContext, ParameterValueMapper.NONE, Collections.emptyList(), filters, Collections.emptyList());
		this.endpointPathMappers = endpointPathMappers;
	}

	@Override
	protected boolean isEndpointTypeExposed(Class<?> beanType) {
		return MergedAnnotations.from(beanType, SearchStrategy.SUPERCLASS).isPresent(ServletEndpoint.class);
	}

	@Override
	protected ExposableServletEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
			Collection<Operation> operations) {
		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, id);
		return new DiscoveredServletEndpoint(this, endpointBean, id, rootPath, defaultAccess);
	}

	@Override
	protected Operation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		throw new IllegalStateException("ServletEndpoints must not declare operations");
	}

	@Override
	protected OperationKey createOperationKey(Operation operation) {
		throw new IllegalStateException("ServletEndpoints must not declare operations");
	}

	@Override
	protected boolean isInvocable(ExposableServletEndpoint endpoint) {
		return true;
	}

	static class ServletEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection().registerType(ServletEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
