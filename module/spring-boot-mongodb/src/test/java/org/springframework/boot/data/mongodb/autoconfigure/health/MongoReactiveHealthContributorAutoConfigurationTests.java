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

package org.springframework.boot.data.mongodb.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.health.MongoHealthContributorAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.health.MongoReactiveHealthContributorAutoConfiguration;
import org.springframework.boot.mongodb.health.MongoHealthIndicator;
import org.springframework.boot.mongodb.health.MongoReactiveHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoReactiveHealthContributorAutoConfiguration}.
 *
 * @author Yulin Qin
 */
class MongoReactiveHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoReactiveAutoConfiguration.class,
				MongoReactiveHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoReactiveHealthIndicator.class)
			.hasBean("mongoHealthContributor"));
	}

	@Test
	void runWithRegularIndicatorShouldOnlyCreateReactiveIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(MongoHealthContributorAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(MongoReactiveHealthIndicator.class)
				.hasBean("mongoHealthContributor")
				.doesNotHaveBean(MongoHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.mongodb.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(MongoReactiveHealthIndicator.class)
				.doesNotHaveBean("mongoHealthContributor"));
	}

}
