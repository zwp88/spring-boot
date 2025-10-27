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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultErrorAttributes}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Yanming Zhou
 */
class DefaultErrorAttributesTests {

	private final DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final WebRequest webRequest = new ServletWebRequest(this.request);

	@Test
	void includeTimeStamp() {
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes.get("timestamp")).isInstanceOf(Date.class);
	}

	@Test
	void specificStatusCode() {
		this.request.setAttribute("jakarta.servlet.error.status_code", 404);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes).containsEntry("error", HttpStatus.NOT_FOUND.getReasonPhrase());
		assertThat(attributes).containsEntry("status", 404);
	}

	@Test
	void missingStatusCode() {
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes).containsEntry("error", "None");
		assertThat(attributes).containsEntry("status", 999);
	}

	@Test
	void mvcError() {
		RuntimeException ex = new RuntimeException("Test");
		ModelAndView modelAndView = this.errorAttributes.resolveException(this.request, mock(HttpServletResponse.class),
				null, ex);
		this.request.setAttribute("jakarta.servlet.error.exception", new RuntimeException("Ignored"));
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(modelAndView).isNull();
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).containsEntry("message", "Test");
	}

	@Test
	void servletErrorWithMessage() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("jakarta.servlet.error.exception", ex);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).containsEntry("message", "Test");
	}

	@Test
	void servletErrorWithoutMessage() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("jakarta.servlet.error.exception", ex);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).doesNotContainKey("message");
	}

	@Test
	void servletMessageWithMessage() {
		this.request.setAttribute("jakarta.servlet.error.message", "Test");
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).containsEntry("message", "Test");
	}

	@Test
	void servletMessageWithoutMessage() {
		this.request.setAttribute("jakarta.servlet.error.message", "Test");
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).doesNotContainKey("message");
	}

	@Test
	void nullExceptionMessage() {
		this.request.setAttribute("jakarta.servlet.error.exception", new RuntimeException());
		this.request.setAttribute("jakarta.servlet.error.message", "Test");
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).containsEntry("message", "Test");
	}

	@Test
	void nullExceptionMessageAndServletMessage() {
		this.request.setAttribute("jakarta.servlet.error.exception", new RuntimeException());
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).containsEntry("message", "No message available");
	}

	@Test
	void unwrapServletException() {
		RuntimeException ex = new RuntimeException("Test");
		ServletException wrapped = new ServletException(new ServletException(ex));
		this.request.setAttribute("jakarta.servlet.error.exception", wrapped);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(wrapped);
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).containsEntry("message", "Test");
	}

	@Test
	void getError() {
		Error error = new OutOfMemoryError("Test error");
		this.request.setAttribute("jakarta.servlet.error.exception", error);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(error);
		assertThat(attributes).doesNotContainKey("exception");
		assertThat(attributes).containsEntry("message", "Test error");
	}

	@Test
	void withBindingErrors() {
		BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new BindException(bindingResult);
		testBindingResult(bindingResult, ex, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
	}

	@Test
	void withoutBindingErrors() {
		BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new BindException(bindingResult);
		testBindingResult(bindingResult, ex, ErrorAttributeOptions.defaults());
	}

	@Test
	void withMethodArgumentNotValidExceptionBindingErrors() {
		Method method = ReflectionUtils.findMethod(String.class, "substring", int.class);
		assertThat(method).isNotNull();
		MethodParameter parameter = new MethodParameter(method, 0);
		BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new MethodArgumentNotValidException(parameter, bindingResult);
		testBindingResult(bindingResult, ex, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
	}

	@Test
	void withHandlerMethodValidationExceptionBindingErrors() {
		Object target = "test";
		Method method = ReflectionUtils.findMethod(String.class, "substring", int.class);
		assertThat(method).isNotNull();
		MethodParameter parameter = new MethodParameter(method, 0);
		MethodValidationResult methodValidationResult = MethodValidationResult.create(target, method,
				List.of(new ParameterValidationResult(parameter, -1,
						List.of(new ObjectError("beginIndex", "beginIndex is negative")), null, null, null,
						(error, sourceType) -> {
							throw new IllegalArgumentException("No source object of the given type");
						})));
		HandlerMethodValidationException ex = new HandlerMethodValidationException(methodValidationResult);
		testErrors(methodValidationResult.getAllErrors(),
				"Validation failed for method='public java.lang.String java.lang.String.substring(int)'. Error count: 1",
				ex, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
	}

	private void testBindingResult(BindingResult bindingResult, Exception ex, ErrorAttributeOptions options) {
		testErrors(bindingResult.getAllErrors(), "Validation failed for object='objectName'. Error count: 1", ex,
				options);
	}

	private void testErrors(List<? extends MessageSourceResolvable> errors, String expectedMessage, Exception ex,
			ErrorAttributeOptions options) {
		this.request.setAttribute("jakarta.servlet.error.exception", ex);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest, options);
		if (options.isIncluded(Include.MESSAGE)) {
			assertThat(attributes).containsEntry("message", expectedMessage);
		}
		else {
			assertThat(attributes).doesNotContainKey("message");
		}
		if (options.isIncluded(Include.BINDING_ERRORS)) {
			assertThat(attributes).containsEntry("errors",
					org.springframework.boot.web.error.Error.wrapIfNecessary(errors));
		}
		else {
			assertThat(attributes).doesNotContainKey("errors");
		}
	}

	@Test
	void withExceptionAttribute() {
		DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("jakarta.servlet.error.exception", ex);
		Map<String, @Nullable Object> attributes = errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.EXCEPTION, Include.MESSAGE));
		assertThat(attributes).containsEntry("exception", RuntimeException.class.getName());
		assertThat(attributes).containsEntry("message", "Test");
	}

	@Test
	void withStackTraceAttribute() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("jakarta.servlet.error.exception", ex);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.STACK_TRACE));
		Object trace = attributes.get("trace");
		assertThat(trace).isNotNull();
		assertThat(trace.toString()).startsWith("java.lang");
	}

	@Test
	void withoutStackTraceAttribute() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("jakarta.servlet.error.exception", ex);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes).doesNotContainKey("trace");
	}

	@Test
	void shouldIncludePathByDefault() {
		this.request.setAttribute("jakarta.servlet.error.request_uri", "path");
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes).containsEntry("path", "path");
	}

	@Test
	void shouldIncludePath() {
		this.request.setAttribute("jakarta.servlet.error.request_uri", "path");
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.PATH));
		assertThat(attributes).containsEntry("path", "path");
	}

	@Test
	void shouldExcludePath() {
		this.request.setAttribute("jakarta.servlet.error.request_uri", "path");
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of());
		assertThat(attributes).doesNotContainEntry("path", "path");
	}

	@Test
	void whenGetMessageIsOverriddenThenMessageAttributeContainsValueReturnedFromIt() {
		Map<String, @Nullable Object> attributes = new DefaultErrorAttributes() {

			@Override
			protected String getMessage(WebRequest webRequest, @Nullable Throwable error) {
				return "custom message";
			}

		}.getErrorAttributes(this.webRequest, ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes).containsEntry("message", "custom message");
	}

	@Test
	void excludeStatus() {
		this.request.setAttribute("jakarta.servlet.error.status_code", 404);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults().excluding(Include.STATUS));
		assertThat(attributes).doesNotContainKey("status");
	}

	@Test
	void excludeError() {
		this.request.setAttribute("jakarta.servlet.error.status_code", 404);
		Map<String, @Nullable Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults().excluding(Include.ERROR));
		assertThat(attributes).doesNotContainKey("error");
	}

}
