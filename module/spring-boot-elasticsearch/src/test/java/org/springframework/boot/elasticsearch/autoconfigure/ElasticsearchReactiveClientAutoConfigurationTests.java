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

package org.springframework.boot.elasticsearch.autoconfigure;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchReactiveClientAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
class ElasticsearchReactiveClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ElasticsearchReactiveClientAutoConfiguration.class));

	@Test
	void configureWithoutRestClientShouldBackOff() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ReactiveElasticsearchClient.class));
	}

	@Test
	void configureWithRestClientShouldCreateTransportAndClient() {
		this.contextRunner.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ReactiveElasticsearchClient.class));
	}

	@Test
	void configureWhenCustomClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(RestClientConfiguration.class, CustomClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ReactiveElasticsearchClient.class)
				.hasBean("customClient"));
	}

	@Configuration(proxyBeanMethods = false)
	static class RestClientConfiguration {

		@Bean
		Rest5Client restClient() {
			return mock(Rest5Client.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClientConfiguration {

		@Bean
		ReactiveElasticsearchClient customClient() {
			return mock(ReactiveElasticsearchClient.class);
		}

	}

}
