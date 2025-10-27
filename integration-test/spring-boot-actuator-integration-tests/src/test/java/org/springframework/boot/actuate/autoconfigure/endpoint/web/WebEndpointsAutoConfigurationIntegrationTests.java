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

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointWebExtension;
import org.springframework.boot.health.actuate.endpoint.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the auto-configuration of web endpoints.
 *
 * @author Andy Wilkinson
 */
class WebEndpointsAutoConfigurationIntegrationTests {

	@Test
	void healthEndpointWebExtensionIsAutoConfigured() {
		servletWebRunner().run((context) -> context.getBean(WebEndpointTestApplication.class));
		servletWebRunner().run((context) -> assertThat(context).hasSingleBean(HealthEndpointWebExtension.class));
	}

	@Test
	void healthEndpointReactiveWebExtensionIsAutoConfigured() {
		reactiveWebRunner()
			.run((context) -> assertThat(context).hasSingleBean(ReactiveHealthEndpointWebExtension.class));
	}

	private WebApplicationContextRunner servletWebRunner() {
		return new WebApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(WebEndpointTestApplication.class))
			.withPropertyValues("management.defaults.metrics.export.enabled=false");
	}

	private ReactiveWebApplicationContextRunner reactiveWebRunner() {
		return new ReactiveWebApplicationContextRunner()
			.withConfiguration(UserConfigurations.of(WebEndpointTestApplication.class))
			.withPropertyValues("management.defaults.metrics.export.enabled=false");
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	static class WebEndpointTestApplication {

	}

}
