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

package org.springframework.boot.webflux.autoconfigure.error;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.webflux.error.DefaultErrorAttributes;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.all;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Basic global {@link org.springframework.web.server.WebExceptionHandler}, rendering
 * {@link ErrorAttributes}.
 * <p>
 * More specific errors can be handled either using Spring WebFlux abstractions (e.g.
 * {@code @ExceptionHandler} with the annotation model) or by adding
 * {@link RouterFunction} to the chain.
 * <p>
 * This implementation will render error as HTML views if the client explicitly supports
 * that media type. It attempts to resolve error views using well known conventions. Will
 * search for templates and static assets under {@code '/error'} using the
 * {@link HttpStatus status code} and the {@link HttpStatus#series() status series}.
 * <p>
 * For example, an {@code HTTP 404} will search (in the specific order):
 * <ul>
 * <li>{@code '/<templates>/error/404.<ext>'}</li>
 * <li>{@code '/<static>/error/404.html'}</li>
 * <li>{@code '/<templates>/error/4xx.<ext>'}</li>
 * <li>{@code '/<static>/error/4xx.html'}</li>
 * <li>{@code '/<templates>/error/error'}</li>
 * <li>{@code '/<static>/error/error.html'}</li>
 * </ul>
 * <p>
 * If none found, a default "Whitelabel Error" HTML view will be rendered.
 * <p>
 * If the client doesn't support HTML, the error information will be rendered as a JSON
 * payload.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public class DefaultErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

	private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

	private static final Map<HttpStatus.Series, String> SERIES_VIEWS;

	static {
		Map<HttpStatus.Series, String> views = new EnumMap<>(HttpStatus.Series.class);
		views.put(HttpStatus.Series.CLIENT_ERROR, "4xx");
		views.put(HttpStatus.Series.SERVER_ERROR, "5xx");
		SERIES_VIEWS = Collections.unmodifiableMap(views);
	}

	private static final ErrorAttributeOptions ONLY_STATUS = ErrorAttributeOptions.of(Include.STATUS);

	private static final DefaultErrorAttributes defaultErrorAttributes = new DefaultErrorAttributes();

	private final ErrorProperties errorProperties;

	/**
	 * Create a new {@code DefaultErrorWebExceptionHandler} instance.
	 * @param errorAttributes the error attributes
	 * @param resources the resources configuration properties
	 * @param errorProperties the error configuration properties
	 * @param applicationContext the current application context
	 */
	public DefaultErrorWebExceptionHandler(ErrorAttributes errorAttributes, Resources resources,
			ErrorProperties errorProperties, ApplicationContext applicationContext) {
		super(errorAttributes, resources, applicationContext);
		this.errorProperties = errorProperties;
	}

	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		return route(acceptsTextHtml(), this::renderErrorView).andRoute(all(), this::renderErrorResponse);
	}

	/**
	 * Render the error information as an HTML view.
	 * @param request the current request
	 * @return a {@code Publisher} of the HTTP response
	 */
	protected Mono<ServerResponse> renderErrorView(ServerRequest request) {
		Map<String, @Nullable Object> errorAttributes = getErrorAttributes(request, MediaType.TEXT_HTML);
		int status = getHttpStatus(request, errorAttributes);
		ServerResponse.BodyBuilder responseBody = ServerResponse.status(status).contentType(TEXT_HTML_UTF8);
		return Flux.just(getData(status).toArray(new String[] {}))
			.flatMap((viewName) -> renderErrorView(viewName, responseBody, errorAttributes))
			.switchIfEmpty(this.errorProperties.getWhitelabel().isEnabled()
					? renderDefaultErrorView(responseBody, errorAttributes) : getErrorMono(request))
			.next();
	}

	private Mono<ServerResponse> getErrorMono(ServerRequest request) {
		Throwable error = getError(request);
		return (error != null) ? Mono.error(error) : Mono.empty();
	}

	private List<String> getData(int errorStatus) {
		List<String> data = new ArrayList<>();
		data.add("error/" + errorStatus);
		HttpStatus.Series series = HttpStatus.Series.resolve(errorStatus);
		if (series != null) {
			data.add("error/" + SERIES_VIEWS.get(series));
		}
		data.add("error/error");
		return data;
	}

	/**
	 * Render the error information as a JSON payload.
	 * @param request the current request
	 * @return a {@code Publisher} of the HTTP response
	 */
	protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		Map<String, @Nullable Object> errorAttributes = getErrorAttributes(request, MediaType.ALL);
		int status = getHttpStatus(request, errorAttributes);
		return ServerResponse.status(status)
			.contentType(MediaType.APPLICATION_JSON)
			.body(BodyInserters.fromValue(errorAttributes));
	}

	private Map<String, @Nullable Object> getErrorAttributes(ServerRequest request, MediaType mediaType) {
		return getErrorAttributes(request, getErrorAttributeOptions(request, mediaType));
	}

	protected ErrorAttributeOptions getErrorAttributeOptions(ServerRequest request, MediaType mediaType) {
		ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
		if (this.errorProperties.isIncludeException()) {
			options = options.including(Include.EXCEPTION);
		}
		if (isIncludeStackTrace(request, mediaType)) {
			options = options.including(Include.STACK_TRACE);
		}
		if (isIncludeMessage(request, mediaType)) {
			options = options.including(Include.MESSAGE);
		}
		if (isIncludeBindingErrors(request, mediaType)) {
			options = options.including(Include.BINDING_ERRORS);
		}
		options = isIncludePath(request, mediaType) ? options.including(Include.PATH) : options.excluding(Include.PATH);
		return options;
	}

	/**
	 * Determine if the stacktrace attribute should be included.
	 * @param request the source request
	 * @param produces the media type produced (or {@code MediaType.ALL})
	 * @return if the stacktrace attribute should be included
	 */
	protected boolean isIncludeStackTrace(ServerRequest request, MediaType produces) {
		return switch (this.errorProperties.getIncludeStacktrace()) {
			case ALWAYS -> true;
			case ON_PARAM -> isTraceEnabled(request);
			case NEVER -> false;
		};
	}

	/**
	 * Determine if the message attribute should be included.
	 * @param request the source request
	 * @param produces the media type produced (or {@code MediaType.ALL})
	 * @return if the message attribute should be included
	 */
	protected boolean isIncludeMessage(ServerRequest request, MediaType produces) {
		return switch (this.errorProperties.getIncludeMessage()) {
			case ALWAYS -> true;
			case ON_PARAM -> isMessageEnabled(request);
			case NEVER -> false;
		};
	}

	/**
	 * Determine if the errors attribute should be included.
	 * @param request the source request
	 * @param produces the media type produced (or {@code MediaType.ALL})
	 * @return if the errors attribute should be included
	 */
	protected boolean isIncludeBindingErrors(ServerRequest request, MediaType produces) {
		return switch (this.errorProperties.getIncludeBindingErrors()) {
			case ALWAYS -> true;
			case ON_PARAM -> isBindingErrorsEnabled(request);
			case NEVER -> false;
		};
	}

	/**
	 * Determine if the path attribute should be included.
	 * @param request the source request
	 * @param produces the media type produced (or {@code MediaType.ALL})
	 * @return if the path attribute should be included
	 */
	protected boolean isIncludePath(ServerRequest request, MediaType produces) {
		return switch (this.errorProperties.getIncludePath()) {
			case ALWAYS -> true;
			case ON_PARAM -> isPathEnabled(request);
			case NEVER -> false;
		};
	}

	private int getHttpStatus(ServerRequest request, Map<String, @Nullable Object> errorAttributes) {
		return getHttpStatus(errorAttributes.containsKey("status") ? errorAttributes
				: defaultErrorAttributes.getErrorAttributes(request, ONLY_STATUS));
	}

	/**
	 * Get the HTTP error status information from the error map.
	 * @param errorAttributes the current error information
	 * @return the error HTTP status
	 */
	protected int getHttpStatus(Map<String, @Nullable Object> errorAttributes) {
		Object status = errorAttributes.get("status");
		Assert.state(status instanceof Integer, "ErrorAttributes must contain a status integer");
		return (int) status;
	}

	/**
	 * Predicate that checks whether the current request explicitly support
	 * {@code "text/html"} media type.
	 * <p>
	 * The "match-all" media type is not considered here.
	 * @return the request predicate
	 */
	protected RequestPredicate acceptsTextHtml() {
		return (serverRequest) -> {
			try {
				List<MediaType> acceptedMediaTypes = serverRequest.headers().accept();
				acceptedMediaTypes.removeIf(MediaType.ALL::equalsTypeAndSubtype);
				MimeTypeUtils.sortBySpecificity(acceptedMediaTypes);
				return acceptedMediaTypes.stream().anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
			}
			catch (InvalidMediaTypeException ex) {
				return false;
			}
		};
	}

}
