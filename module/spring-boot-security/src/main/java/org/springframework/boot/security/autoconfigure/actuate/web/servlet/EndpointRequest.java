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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.security.web.servlet.ApplicationContextRequestMatcher;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Factory that can be used to create a {@link RequestMatcher} for actuator endpoint
 * locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Chris Bono
 * @since 4.0.0
 */
public final class EndpointRequest {

	private static final RequestMatcher EMPTY_MATCHER = (request) -> false;

	private EndpointRequest() {
	}

	/**
	 * Returns a matcher that includes all {@link Endpoint actuator endpoints}. It also
	 * includes the links endpoint which is present at the base path of the actuator
	 * endpoints. The {@link EndpointRequestMatcher#excluding(Class...) excluding} method
	 * can be used to further remove specific endpoints if required. For example:
	 * <pre class="code">
	 * EndpointRequest.toAnyEndpoint().excluding(ShutdownEndpoint.class)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher toAnyEndpoint() {
		return new EndpointRequestMatcher(true);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to(ShutdownEndpoint.class, HealthEndpoint.class)
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(Class<?>... endpoints) {
		return new EndpointRequestMatcher(endpoints, false);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to("shutdown", "health")
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(String... endpoints) {
		return new EndpointRequestMatcher(endpoints, false);
	}

	/**
	 * Returns a matcher that matches only on the links endpoint. It can be used when
	 * security configuration for the links endpoint is different from the other
	 * {@link Endpoint actuator endpoints}. The
	 * {@link EndpointRequestMatcher#excludingLinks() excludingLinks} method can be used
	 * in combination with this to remove the links endpoint from
	 * {@link EndpointRequest#toAnyEndpoint() toAnyEndpoint}. For example:
	 * <pre class="code">
	 * EndpointRequest.toLinks()
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static LinksRequestMatcher toLinks() {
		return new LinksRequestMatcher();
	}

	/**
	 * Returns a matcher that includes additional paths under a {@link WebServerNamespace}
	 * for the specified {@link Endpoint actuator endpoints}. For example:
	 * <pre class="code">
	 * EndpointRequest.toAdditionalPaths(WebServerNamespace.SERVER, "health")
	 * </pre>
	 * @param webServerNamespace the web server namespace
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static AdditionalPathsEndpointRequestMatcher toAdditionalPaths(WebServerNamespace webServerNamespace,
			Class<?>... endpoints) {
		return new AdditionalPathsEndpointRequestMatcher(webServerNamespace, endpoints);
	}

	/**
	 * Returns a matcher that includes additional paths under a {@link WebServerNamespace}
	 * for the specified {@link Endpoint actuator endpoints}. For example:
	 * <pre class="code">
	 * EndpointRequest.toAdditionalPaths(WebServerNamespace.SERVER, HealthEndpoint.class)
	 * </pre>
	 * @param webServerNamespace the web server namespace
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static AdditionalPathsEndpointRequestMatcher toAdditionalPaths(WebServerNamespace webServerNamespace,
			String... endpoints) {
		return new AdditionalPathsEndpointRequestMatcher(webServerNamespace, endpoints);
	}

	/**
	 * Base class for supported request matchers.
	 */
	private abstract static class AbstractRequestMatcher
			extends ApplicationContextRequestMatcher<WebApplicationContext> {

		private volatile @Nullable RequestMatcher delegate;

		private volatile @Nullable ManagementPortType managementPortType;

		AbstractRequestMatcher() {
			super(WebApplicationContext.class);
		}

		@Override
		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext) {
			ManagementPortType managementPortType = this.managementPortType;
			if (managementPortType == null) {
				managementPortType = ManagementPortType.get(applicationContext.getEnvironment());
				this.managementPortType = managementPortType;
			}
			return ignoreApplicationContext(applicationContext, managementPortType);
		}

		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext,
				ManagementPortType managementPortType) {
			return managementPortType == ManagementPortType.DIFFERENT
					&& !hasWebServerNamespace(applicationContext, WebServerNamespace.MANAGEMENT);
		}

		protected final boolean hasWebServerNamespace(ApplicationContext applicationContext,
				WebServerNamespace webServerNamespace) {
			return WebServerApplicationContext.hasServerNamespace(applicationContext, webServerNamespace.getValue())
					|| hasImplicitServerNamespace(applicationContext, webServerNamespace);
		}

		private boolean hasImplicitServerNamespace(ApplicationContext applicationContext,
				WebServerNamespace webServerNamespace) {
			return WebServerNamespace.SERVER.equals(webServerNamespace)
					&& WebServerApplicationContext.getServerNamespace(applicationContext) == null
					&& applicationContext.getParent() == null;
		}

		@Override
		protected final void initialized(Supplier<WebApplicationContext> context) {
			this.delegate = createDelegate(context.get());
		}

		@Override
		protected final boolean matches(HttpServletRequest request, Supplier<WebApplicationContext> context) {
			RequestMatcher delegate = this.delegate;
			Assert.state(delegate != null, "'delegate' must not be null");
			return delegate.matches(request);
		}

		private RequestMatcher createDelegate(WebApplicationContext context) {
			try {
				return createDelegate(context, new RequestMatcherFactory());
			}
			catch (NoSuchBeanDefinitionException ex) {
				return EMPTY_MATCHER;
			}
		}

		protected abstract RequestMatcher createDelegate(WebApplicationContext context,
				RequestMatcherFactory requestMatcherFactory);

		protected final List<RequestMatcher> getDelegateMatchers(RequestMatcherFactory requestMatcherFactory,
				RequestMatcherProvider matcherProvider, Set<String> paths, @Nullable HttpMethod httpMethod) {
			return paths.stream()
				.map((path) -> requestMatcherFactory.antPath(matcherProvider, httpMethod, path, "/**"))
				.collect(Collectors.toCollection(ArrayList::new));
		}

		protected List<RequestMatcher> getLinksMatchers(RequestMatcherFactory requestMatcherFactory,
				RequestMatcherProvider matcherProvider, String basePath) {
			List<RequestMatcher> linksMatchers = new ArrayList<>();
			linksMatchers.add(requestMatcherFactory.antPath(matcherProvider, null, basePath));
			linksMatchers.add(requestMatcherFactory.antPath(matcherProvider, null, basePath, "/"));
			return linksMatchers;
		}

		protected RequestMatcherProvider getRequestMatcherProvider(WebApplicationContext context) {
			try {
				return context.getBean(RequestMatcherProvider.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return (pattern, method) -> PathPatternRequestMatcher.withDefaults().matcher(method, pattern);
			}
		}

		protected String toString(List<Object> endpoints, String emptyValue) {
			return (!endpoints.isEmpty()) ? endpoints.stream()
				.map(this::getEndpointId)
				.map(Object::toString)
				.collect(Collectors.joining(", ", "[", "]")) : emptyValue;
		}

		protected EndpointId getEndpointId(Object source) {
			if (source instanceof EndpointId endpointId) {
				return endpointId;
			}
			if (source instanceof String string) {
				return EndpointId.of(string);
			}
			if (source instanceof Class<?> sourceClass) {
				return getEndpointId(sourceClass);
			}
			throw new IllegalStateException("Unsupported source " + source);
		}

		private EndpointId getEndpointId(Class<?> source) {
			MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(source).get(Endpoint.class);
			Assert.state(annotation.isPresent(), () -> "Class " + source + " is not annotated with @Endpoint");
			return EndpointId.of(annotation.getString("id"));
		}

	}

	/**
	 * The request matcher used to match against {@link Endpoint actuator endpoints}.
	 */
	public static final class EndpointRequestMatcher extends AbstractRequestMatcher {

		private final List<Object> includes;

		private final List<Object> excludes;

		private final boolean includeLinks;

		private final @Nullable HttpMethod httpMethod;

		private EndpointRequestMatcher(boolean includeLinks) {
			this(Collections.emptyList(), Collections.emptyList(), includeLinks, null);
		}

		private EndpointRequestMatcher(Class<?>[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks, null);
		}

		private EndpointRequestMatcher(String[] endpoints, boolean includeLinks) {
			this(Arrays.asList((Object[]) endpoints), Collections.emptyList(), includeLinks, null);
		}

		private EndpointRequestMatcher(List<Object> includes, List<Object> excludes, boolean includeLinks,
				@Nullable HttpMethod httpMethod) {
			this.includes = includes;
			this.excludes = excludes;
			this.includeLinks = includeLinks;
			this.httpMethod = httpMethod;
		}

		public EndpointRequestMatcher excluding(Class<?>... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes, this.includeLinks, null);
		}

		public EndpointRequestMatcher excluding(String... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes, this.includeLinks, null);
		}

		public EndpointRequestMatcher excludingLinks() {
			return new EndpointRequestMatcher(this.includes, this.excludes, false, null);
		}

		/**
		 * Restricts the matcher to only consider requests with a particular HTTP method.
		 * @param httpMethod the HTTP method to include
		 * @return a copy of the matcher further restricted to only match requests with
		 * the specified HTTP method
		 */
		public EndpointRequestMatcher withHttpMethod(HttpMethod httpMethod) {
			return new EndpointRequestMatcher(this.includes, this.excludes, this.includeLinks, httpMethod);
		}

		@Override
		protected RequestMatcher createDelegate(WebApplicationContext context,
				RequestMatcherFactory requestMatcherFactory) {
			PathMappedEndpoints endpoints = context.getBean(PathMappedEndpoints.class);
			RequestMatcherProvider matcherProvider = getRequestMatcherProvider(context);
			Set<String> paths = new LinkedHashSet<>();
			if (this.includes.isEmpty()) {
				paths.addAll(endpoints.getAllPaths());
			}
			streamPaths(this.includes, endpoints).forEach(paths::add);
			streamPaths(this.excludes, endpoints).forEach(paths::remove);
			List<RequestMatcher> delegateMatchers = getDelegateMatchers(requestMatcherFactory, matcherProvider, paths,
					this.httpMethod);
			String basePath = endpoints.getBasePath();
			if (this.includeLinks && StringUtils.hasText(basePath)) {
				delegateMatchers.addAll(getLinksMatchers(requestMatcherFactory, matcherProvider, basePath));
			}
			if (delegateMatchers.isEmpty()) {
				return EMPTY_MATCHER;
			}
			return new OrRequestMatcher(delegateMatchers);
		}

		private Stream<String> streamPaths(List<Object> source, PathMappedEndpoints endpoints) {
			return source.stream()
				.filter(Objects::nonNull)
				.map(this::getEndpointId)
				.map(endpoints::getPath)
				.filter(Objects::nonNull);
		}

		@Override
		public String toString() {
			return String.format("EndpointRequestMatcher includes=%s, excludes=%s, includeLinks=%s",
					toString(this.includes, "[*]"), toString(this.excludes, "[]"), this.includeLinks);
		}

	}

	/**
	 * The request matcher used to match against the links endpoint.
	 */
	public static final class LinksRequestMatcher extends AbstractRequestMatcher {

		@Override
		protected RequestMatcher createDelegate(WebApplicationContext context,
				RequestMatcherFactory requestMatcherFactory) {
			WebEndpointProperties properties = context.getBean(WebEndpointProperties.class);
			String basePath = properties.getBasePath();
			if (StringUtils.hasText(basePath)) {
				return new OrRequestMatcher(
						getLinksMatchers(requestMatcherFactory, getRequestMatcherProvider(context), basePath));
			}
			return EMPTY_MATCHER;
		}

		@Override
		public String toString() {
			return String.format("LinksRequestMatcher");
		}

	}

	/**
	 * The request matcher used to match against additional paths for {@link Endpoint
	 * actuator endpoints}.
	 */
	public static class AdditionalPathsEndpointRequestMatcher extends AbstractRequestMatcher {

		private final WebServerNamespace webServerNamespace;

		private final List<Object> endpoints;

		private final @Nullable HttpMethod httpMethod;

		AdditionalPathsEndpointRequestMatcher(WebServerNamespace webServerNamespace, String... endpoints) {
			this(webServerNamespace, Arrays.asList((Object[]) endpoints), null);
		}

		AdditionalPathsEndpointRequestMatcher(WebServerNamespace webServerNamespace, Class<?>... endpoints) {
			this(webServerNamespace, Arrays.asList((Object[]) endpoints), null);
		}

		private AdditionalPathsEndpointRequestMatcher(WebServerNamespace webServerNamespace, List<Object> endpoints,
				@Nullable HttpMethod httpMethod) {
			Assert.notNull(webServerNamespace, "'webServerNamespace' must not be null");
			Assert.notNull(endpoints, "'endpoints' must not be null");
			Assert.notEmpty(endpoints, "'endpoints' must not be empty");
			this.webServerNamespace = webServerNamespace;
			this.endpoints = endpoints;
			this.httpMethod = httpMethod;
		}

		/**
		 * Restricts the matcher to only consider requests with a particular HTTP method.
		 * @param httpMethod the HTTP method to include
		 * @return a copy of the matcher further restricted to only match requests with
		 * the specified HTTP method
		 */
		public AdditionalPathsEndpointRequestMatcher withHttpMethod(HttpMethod httpMethod) {
			return new AdditionalPathsEndpointRequestMatcher(this.webServerNamespace, this.endpoints, httpMethod);
		}

		@Override
		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext,
				ManagementPortType managementPortType) {
			return !hasWebServerNamespace(applicationContext, this.webServerNamespace);
		}

		@Override
		protected RequestMatcher createDelegate(WebApplicationContext context,
				RequestMatcherFactory requestMatcherFactory) {
			PathMappedEndpoints endpoints = context.getBean(PathMappedEndpoints.class);
			RequestMatcherProvider matcherProvider = getRequestMatcherProvider(context);
			Set<String> paths = this.endpoints.stream()
				.filter(Objects::nonNull)
				.map(this::getEndpointId)
				.flatMap((endpointId) -> streamAdditionalPaths(endpoints, endpointId))
				.collect(Collectors.toCollection(LinkedHashSet::new));
			List<RequestMatcher> delegateMatchers = getDelegateMatchers(requestMatcherFactory, matcherProvider, paths,
					this.httpMethod);
			return (!CollectionUtils.isEmpty(delegateMatchers)) ? new OrRequestMatcher(delegateMatchers)
					: EMPTY_MATCHER;
		}

		private Stream<String> streamAdditionalPaths(PathMappedEndpoints pathMappedEndpoints, EndpointId endpointId) {
			return pathMappedEndpoints.getAdditionalPaths(this.webServerNamespace, endpointId).stream();
		}

		@Override
		public String toString() {
			return String.format("AdditionalPathsEndpointRequestMatcher endpoints=%s, webServerNamespace=%s",
					toString(this.endpoints, ""), this.webServerNamespace);
		}

	}

	/**
	 * Factory used to create a {@link RequestMatcher}.
	 */
	private static final class RequestMatcherFactory {

		RequestMatcher antPath(RequestMatcherProvider matcherProvider, @Nullable HttpMethod httpMethod,
				String... parts) {
			StringBuilder pattern = new StringBuilder();
			for (String part : parts) {
				Assert.notNull(part, "'part' must not be null");
				pattern.append(part);
			}
			return matcherProvider.getRequestMatcher(pattern.toString(), httpMethod);
		}

	}

}
