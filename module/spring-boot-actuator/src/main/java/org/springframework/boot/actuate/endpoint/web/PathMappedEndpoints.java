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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A collection of {@link PathMappedEndpoint path mapped endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class PathMappedEndpoints implements Iterable<PathMappedEndpoint> {

	private final String basePath;

	private final Map<EndpointId, PathMappedEndpoint> endpoints;

	/**
	 * Create a new {@link PathMappedEndpoints} instance for the given supplier.
	 * @param basePath the base path of the endpoints
	 * @param supplier the endpoint supplier
	 */
	public PathMappedEndpoints(@Nullable String basePath, EndpointsSupplier<?> supplier) {
		Assert.notNull(supplier, "'supplier' must not be null");
		this.basePath = (basePath != null) ? basePath : "";
		this.endpoints = getEndpoints(Collections.singleton(supplier));
	}

	/**
	 * Create a new {@link PathMappedEndpoints} instance for the given suppliers.
	 * @param basePath the base path of the endpoints
	 * @param suppliers the endpoint suppliers
	 */
	public PathMappedEndpoints(@Nullable String basePath, Collection<EndpointsSupplier<?>> suppliers) {
		Assert.notNull(suppliers, "'suppliers' must not be null");
		this.basePath = (basePath != null) ? basePath : "";
		this.endpoints = getEndpoints(suppliers);
	}

	private Map<EndpointId, PathMappedEndpoint> getEndpoints(Collection<EndpointsSupplier<?>> suppliers) {
		Map<EndpointId, PathMappedEndpoint> endpoints = new LinkedHashMap<>();
		suppliers.forEach((supplier) -> supplier.getEndpoints().forEach((endpoint) -> {
			if (endpoint instanceof PathMappedEndpoint pathMappedEndpoint) {
				endpoints.put(endpoint.getEndpointId(), pathMappedEndpoint);
			}
		}));
		return Collections.unmodifiableMap(endpoints);
	}

	/**
	 * Return the base path for the endpoints.
	 * @return the base path
	 */
	public String getBasePath() {
		return this.basePath;
	}

	/**
	 * Return the root path for the endpoint with the given ID or {@code null} if the
	 * endpoint cannot be found.
	 * @param endpointId the endpoint ID
	 * @return the root path or {@code null}
	 */
	public @Nullable String getRootPath(EndpointId endpointId) {
		PathMappedEndpoint endpoint = getEndpoint(endpointId);
		return (endpoint != null) ? endpoint.getRootPath() : null;
	}

	/**
	 * Return the full path for the endpoint with the given ID or {@code null} if the
	 * endpoint cannot be found.
	 * @param endpointId the endpoint ID
	 * @return the full path or {@code null}
	 */
	public @Nullable String getPath(EndpointId endpointId) {
		return getPath(getEndpoint(endpointId));
	}

	/**
	 * Return the root paths for each mapped endpoint (excluding additional paths).
	 * @return all root paths
	 */
	public Collection<String> getAllRootPaths() {
		return stream().map(PathMappedEndpoint::getRootPath).toList();
	}

	/**
	 * Return the full paths for each mapped endpoint (excluding additional paths).
	 * @return all root paths
	 */
	public Collection<String> getAllPaths() {
		return stream().map(this::getPath).toList();
	}

	/**
	 * Return the additional paths for each mapped endpoint.
	 * @param webServerNamespace the web server namespace
	 * @param endpointId the endpoint ID
	 * @return all additional paths
	 * @since 3.4.0
	 */
	public Collection<String> getAdditionalPaths(WebServerNamespace webServerNamespace, EndpointId endpointId) {
		return getAdditionalPaths(webServerNamespace, getEndpoint(endpointId)).toList();
	}

	private Stream<String> getAdditionalPaths(WebServerNamespace webServerNamespace,
			@Nullable PathMappedEndpoint endpoint) {
		List<String> additionalPaths = (endpoint != null) ? endpoint.getAdditionalPaths(webServerNamespace) : null;
		if (CollectionUtils.isEmpty(additionalPaths)) {
			return Stream.empty();
		}
		return additionalPaths.stream().map(this::getAdditionalPath);
	}

	private String getAdditionalPath(String path) {
		return path.startsWith("/") ? path : "/" + path;
	}

	/**
	 * Return the {@link PathMappedEndpoint} with the given ID or {@code null} if the
	 * endpoint cannot be found.
	 * @param endpointId the endpoint ID
	 * @return the path mapped endpoint or {@code null}
	 */
	public @Nullable PathMappedEndpoint getEndpoint(EndpointId endpointId) {
		return this.endpoints.get(endpointId);
	}

	/**
	 * Stream all {@link PathMappedEndpoint path mapped endpoints}.
	 * @return a stream of endpoints
	 */
	public Stream<PathMappedEndpoint> stream() {
		return this.endpoints.values().stream();
	}

	@Override
	public Iterator<PathMappedEndpoint> iterator() {
		return this.endpoints.values().iterator();
	}

	@Contract("!null -> !null")
	private @Nullable String getPath(@Nullable PathMappedEndpoint endpoint) {
		if (endpoint == null) {
			return null;
		}
		StringBuilder path = new StringBuilder(this.basePath);
		if (!this.basePath.equals("/")) {
			path.append("/");
		}
		if (!endpoint.getRootPath().equals("/")) {
			path.append(endpoint.getRootPath());
		}
		return path.toString();
	}

}
