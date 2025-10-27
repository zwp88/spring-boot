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

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServlet;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import org.springframework.boot.actuate.autoconfigure.audit.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Actuator's MVC endpoints.
 *
 * @author Andy Wilkinson
 */
class WebMvcEndpointIntegrationTests {

	private @Nullable AnnotationConfigServletWebApplicationContext context;

	@Test
	void webMvcEndpointHandlerMappingIsConfiguredWithPathPatternParser() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		WebMvcEndpointHandlerMapping handlerMapping = this.context.getBean(WebMvcEndpointHandlerMapping.class);
		assertThat(handlerMapping.getPatternParser()).isInstanceOf(PathPatternParser.class);
	}

	@Test
	void linksAreProvidedToAllEndpointTypes() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class, EndpointsConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.exposure.include=*").applyTo(this.context);
		MockMvcTester mvc = doCreateMockMvcTester();
		assertThat(mvc.get().uri("/actuator").accept("*/*")).hasStatusOk()
			.bodyJson()
			.extractingPath("_links")
			.asMap()
			.containsKeys("beans", "servlet", "restcontroller", "controller");
	}

	@Test
	void linksPageIsNotAvailableWhenDisabled() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class, EndpointsConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.discovery.enabled=false").applyTo(this.context);
		MockMvcTester mvc = doCreateMockMvcTester();
		assertThat(mvc.get().uri("/actuator").accept("*/*")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void endpointJsonMapperCanBeApplied() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(EndpointJsonMapperConfiguration.class, DefaultConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.exposure.include=*").applyTo(this.context);
		MockMvcTester mvc = doCreateMockMvcTester();
		assertThat(mvc.get().uri("/actuator/beans")).hasStatusOk().bodyText().contains("\"scope\":\"notelgnis\"");
	}

	private MockMvcTester doCreateMockMvcTester() {
		assertThat(this.context).isNotNull();
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		return MockMvcTester.from(this.context);
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
			ServletManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, BeansEndpointAutoConfiguration.class })
	static class DefaultConfiguration {

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint(id = "servlet")
	@SuppressWarnings({ "deprecation", "removal" })
	static class TestServletEndpoint
			implements Supplier<org.springframework.boot.actuate.endpoint.web.EndpointServlet> {

		@Override
		public org.springframework.boot.actuate.endpoint.web.EndpointServlet get() {
			return new org.springframework.boot.actuate.endpoint.web.EndpointServlet(new HttpServlet() {
			});
		}

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint(id = "controller")
	@SuppressWarnings("removal")
	static class TestControllerEndpoint {

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint(id = "restcontroller")
	@SuppressWarnings("removal")
	static class TestRestControllerEndpoint {

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointsConfiguration {

		@Bean
		TestServletEndpoint testServletEndpoint() {
			return new TestServletEndpoint();
		}

		@Bean
		TestControllerEndpoint testControllerEndpoint() {
			return new TestControllerEndpoint();
		}

		@Bean
		TestRestControllerEndpoint testRestControllerEndpoint() {
			return new TestRestControllerEndpoint();
		}

	}

	@Configuration
	static class EndpointJsonMapperConfiguration {

		@Bean
		EndpointJsonMapper endpointJsonMapper() {
			SimpleModule module = new SimpleModule();
			module.addSerializer(String.class, new ReverseStringSerializer());
			JsonMapper jsonMapper = JsonMapper.builder().addModule(module).build();
			return () -> jsonMapper;
		}

		static class ReverseStringSerializer extends StdScalarSerializer<Object> {

			ReverseStringSerializer() {
				super(String.class, false);
			}

			@Override
			public boolean isEmpty(SerializationContext context, Object value) {
				return ((String) value).isEmpty();
			}

			@Override
			public void serialize(Object value, JsonGenerator gen, SerializationContext context) {
				serialize(value, gen);
			}

			@Override
			public final void serializeWithType(Object value, JsonGenerator gen, SerializationContext context,
					TypeSerializer typeSer) {
				serialize(value, gen);
			}

			private void serialize(Object value, JsonGenerator gen) {
				StringBuilder builder = new StringBuilder((String) value);
				gen.writeString(builder.reverse().toString());
			}

		}

	}

}
