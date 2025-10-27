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

package org.springframework.boot.webmvc.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.error.Error;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default implementation of {@link ErrorAttributes}. Provides the following attributes
 * when possible:
 * <ul>
 * <li>timestamp - The time that the errors were extracted</li>
 * <li>status - The status code</li>
 * <li>error - The error reason</li>
 * <li>exception - The class name of the root exception (if configured)</li>
 * <li>message - The exception message (if configured)</li>
 * <li>errors - Any validation errors derived from a {@link BindingResult} or
 * {@link MethodValidationResult} exception (if configured). To ensure safe serialization
 * to JSON, errors are {@link Error#wrapIfNecessary(java.util.List) wrapped if
 * necessary}</li>
 * <li>trace - The exception stack trace (if configured)</li>
 * <li>path - The URL path when the exception was raised</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Yanming Zhou
 * @author Yongjun Hong
 * @since 4.0.0
 * @see ErrorAttributes
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultErrorAttributes implements ErrorAttributes, HandlerExceptionResolver, Ordered {

	private static final String ERROR_INTERNAL_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public @Nullable ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
			@Nullable Object handler, Exception ex) {
		storeErrorAttributes(request, ex);
		return null;
	}

	private void storeErrorAttributes(HttpServletRequest request, Exception ex) {
		request.setAttribute(ERROR_INTERNAL_ATTRIBUTE, ex);
	}

	@Override
	public Map<String, @Nullable Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
		Map<String, @Nullable Object> errorAttributes = getErrorAttributes(webRequest,
				options.isIncluded(Include.STACK_TRACE));
		options.retainIncluded(errorAttributes);
		return errorAttributes;
	}

	private Map<String, @Nullable Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
		Map<String, @Nullable Object> errorAttributes = new LinkedHashMap<>();
		errorAttributes.put("timestamp", new Date());
		addStatus(errorAttributes, webRequest);
		addErrorDetails(errorAttributes, webRequest, includeStackTrace);
		addPath(errorAttributes, webRequest);
		return errorAttributes;
	}

	private void addStatus(Map<String, @Nullable Object> errorAttributes, RequestAttributes requestAttributes) {
		Integer status = getAttribute(requestAttributes, RequestDispatcher.ERROR_STATUS_CODE);
		if (status == null) {
			errorAttributes.put("status", 999);
			errorAttributes.put("error", "None");
			return;
		}
		errorAttributes.put("status", status);
		try {
			errorAttributes.put("error", HttpStatus.valueOf(status).getReasonPhrase());
		}
		catch (Exception ex) {
			// Unable to obtain a reason
			errorAttributes.put("error", "Http Status " + status);
		}
	}

	private void addErrorDetails(Map<String, @Nullable Object> errorAttributes, WebRequest webRequest,
			boolean includeStackTrace) {
		Throwable error = getError(webRequest);
		if (error != null) {
			while (error instanceof ServletException && error.getCause() != null) {
				error = error.getCause();
			}
			errorAttributes.put("exception", error.getClass().getName());
			if (includeStackTrace) {
				addStackTrace(errorAttributes, error);
			}
		}
		addErrorMessage(errorAttributes, webRequest, error);
	}

	private void addErrorMessage(Map<String, @Nullable Object> errorAttributes, WebRequest webRequest,
			@Nullable Throwable error) {
		BindingResult bindingResult = extractBindingResult(error);
		if (bindingResult != null) {
			addMessageAndErrorsFromBindingResult(errorAttributes, bindingResult);
			return;
		}
		MethodValidationResult methodValidationResult = extractMethodValidationResult(error);
		if (methodValidationResult != null) {
			addMessageAndErrorsFromMethodValidationResult(errorAttributes, methodValidationResult);
			return;
		}
		addExceptionErrorMessage(errorAttributes, webRequest, error);
	}

	private void addMessageAndErrorsFromBindingResult(Map<String, @Nullable Object> errorAttributes,
			BindingResult result) {
		errorAttributes.put("message", "Validation failed for object='%s'. Error count: %s"
			.formatted(result.getObjectName(), result.getAllErrors().size()));
		errorAttributes.put("errors", Error.wrapIfNecessary(result.getAllErrors()));
	}

	private void addMessageAndErrorsFromMethodValidationResult(Map<String, @Nullable Object> errorAttributes,
			MethodValidationResult result) {
		errorAttributes.put("message", "Validation failed for method='%s'. Error count: %s"
			.formatted(result.getMethod(), result.getAllErrors().size()));
		errorAttributes.put("errors", Error.wrapIfNecessary(result.getAllErrors()));
	}

	private void addExceptionErrorMessage(Map<String, @Nullable Object> errorAttributes, WebRequest webRequest,
			@Nullable Throwable error) {
		errorAttributes.put("message", getMessage(webRequest, error));
	}

	/**
	 * Returns the message to be included as the value of the {@code message} error
	 * attribute. By default the returned message is the first of the following that is
	 * not empty:
	 * <ol>
	 * <li>Value of the {@link RequestDispatcher#ERROR_MESSAGE} request attribute.
	 * <li>Message of the given {@code error}.
	 * <li>{@code No message available}.
	 * </ol>
	 * @param webRequest current request
	 * @param error current error, if any
	 * @return message to include in the error attributes
	 */
	protected String getMessage(WebRequest webRequest, @Nullable Throwable error) {
		Object message = getAttribute(webRequest, RequestDispatcher.ERROR_MESSAGE);
		if (!ObjectUtils.isEmpty(message)) {
			return message.toString();
		}
		if (error != null && StringUtils.hasLength(error.getMessage())) {
			return error.getMessage();
		}
		return "No message available";
	}

	private @Nullable BindingResult extractBindingResult(@Nullable Throwable error) {
		if (error instanceof BindingResult bindingResult) {
			return bindingResult;
		}
		return null;
	}

	private @Nullable MethodValidationResult extractMethodValidationResult(@Nullable Throwable error) {
		if (error instanceof MethodValidationResult methodValidationResult) {
			return methodValidationResult;
		}
		return null;
	}

	private void addStackTrace(Map<String, @Nullable Object> errorAttributes, Throwable error) {
		StringWriter stackTrace = new StringWriter();
		error.printStackTrace(new PrintWriter(stackTrace));
		stackTrace.flush();
		errorAttributes.put("trace", stackTrace.toString());
	}

	private void addPath(Map<String, @Nullable Object> errorAttributes, RequestAttributes requestAttributes) {
		String path = getAttribute(requestAttributes, RequestDispatcher.ERROR_REQUEST_URI);
		if (path != null) {
			errorAttributes.put("path", path);
		}
	}

	@Override
	public @Nullable Throwable getError(WebRequest webRequest) {
		Throwable exception = getAttribute(webRequest, ERROR_INTERNAL_ATTRIBUTE);
		if (exception == null) {
			exception = getAttribute(webRequest, RequestDispatcher.ERROR_EXCEPTION);
		}
		return exception;
	}

	@SuppressWarnings("unchecked")
	private <T> @Nullable T getAttribute(RequestAttributes requestAttributes, String name) {
		return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
	}

}
