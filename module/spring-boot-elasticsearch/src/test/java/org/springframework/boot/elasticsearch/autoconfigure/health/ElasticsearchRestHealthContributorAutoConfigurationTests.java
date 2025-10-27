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

package org.springframework.boot.elasticsearch.autoconfigure.health;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.elasticsearch.health.ElasticsearchRestClientHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchRestHealthContributorAutoConfiguration}.
 *
 * @author Filip Hrisafov
 * @author Andy Wilkinson
 */
class ElasticsearchRestHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class,
				ElasticsearchRestHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ElasticsearchRestClientHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor"));
	}

	@Test
	void runWithoutRestClientShouldNotCreateIndicator() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Rest5Client.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ElasticsearchRestClientHealthIndicator.class)
				.doesNotHaveBean("elasticsearchHealthContributor"));
	}

	@Test
	void runWithRestClientShouldCreateIndicator() {
		this.contextRunner.withUserConfiguration(CustomRestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ElasticsearchRestClientHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor"));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.elasticsearch.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(ElasticsearchRestClientHealthIndicator.class)
				.doesNotHaveBean("elasticsearchHealthContributor"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestClientConfiguration {

		@Bean
		Rest5Client customRestClient(Rest5ClientBuilder builder) {
			return builder.build();
		}

	}

}
