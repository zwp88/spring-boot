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

package org.springframework.boot.restclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RootUriTemplateHandler}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class RootUriTemplateHandlerTests {

	private URI uri;

	@Mock
	@SuppressWarnings("NullAway.Init")
	public UriTemplateHandler delegate;

	public UriTemplateHandler handler;

	@BeforeEach
	void setup() throws URISyntaxException {
		this.uri = new URI("https://example.com/hello");
		this.handler = new RootUriTemplateHandler("https://example.com", this.delegate);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWithNullRootUriShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new RootUriTemplateHandler((String) null, mock(UriTemplateHandler.class)))
			.withMessageContaining("'rootUri' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWithNullHandlerShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RootUriTemplateHandler("https://example.com", null))
			.withMessageContaining("'handler' must not be null");
	}

	@Test
	@SuppressWarnings("unchecked")
	void expandMapVariablesShouldPrefixRoot() {
		given(this.delegate.expand(anyString(), any(Map.class))).willReturn(this.uri);
		HashMap<String, Object> uriVariables = new HashMap<>();
		URI expanded = this.handler.expand("/hello", uriVariables);
		then(this.delegate).should().expand("https://example.com/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

	@Test
	@SuppressWarnings("unchecked")
	void expandMapVariablesWhenPathDoesNotStartWithSlashShouldNotPrefixRoot() {
		given(this.delegate.expand(anyString(), any(Map.class))).willReturn(this.uri);
		HashMap<String, Object> uriVariables = new HashMap<>();
		URI expanded = this.handler.expand("https://spring.io/hello", uriVariables);
		then(this.delegate).should().expand("https://spring.io/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

	@Test
	void expandArrayVariablesShouldPrefixRoot() {
		given(this.delegate.expand(anyString(), any(Object[].class))).willReturn(this.uri);
		Object[] uriVariables = new Object[0];
		URI expanded = this.handler.expand("/hello", uriVariables);
		then(this.delegate).should().expand("https://example.com/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

	@Test
	void expandArrayVariablesWhenPathDoesNotStartWithSlashShouldNotPrefixRoot() {
		given(this.delegate.expand(anyString(), any(Object[].class))).willReturn(this.uri);
		Object[] uriVariables = new Object[0];
		URI expanded = this.handler.expand("https://spring.io/hello", uriVariables);
		then(this.delegate).should().expand("https://spring.io/hello", uriVariables);
		assertThat(expanded).isEqualTo(this.uri);
	}

}
