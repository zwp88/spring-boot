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

package org.springframework.boot.mongodb.autoconfigure.metrics;

import com.mongodb.MongoClientSettings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoCommandTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mongo metrics.
 *
 * @author Chris Bono
 * @author Jonatan Ivanov
 * @since 4.0.0
 */
@AutoConfiguration(before = MongoAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ MongoClientSettings.class, MeterRegistry.class })
@ConditionalOnBean(MeterRegistry.class)
public final class MongoMetricsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MongoMetricsCommandListener.class)
	@ConditionalOnBooleanProperty(name = "management.metrics.mongodb.command.enabled", matchIfMissing = true)
	static class MongoCommandMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		MongoMetricsCommandListener mongoMetricsCommandListener(MeterRegistry meterRegistry,
				MongoCommandTagsProvider mongoCommandTagsProvider) {
			return new MongoMetricsCommandListener(meterRegistry, mongoCommandTagsProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		MongoCommandTagsProvider mongoCommandTagsProvider() {
			return new DefaultMongoCommandTagsProvider();
		}

		@Bean
		MongoClientSettingsBuilderCustomizer mongoMetricsCommandListenerClientSettingsBuilderCustomizer(
				MongoMetricsCommandListener mongoMetricsCommandListener) {
			return (clientSettingsBuilder) -> clientSettingsBuilder.addCommandListener(mongoMetricsCommandListener);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MongoMetricsConnectionPoolListener.class)
	@ConditionalOnBooleanProperty(name = "management.metrics.mongodb.connectionpool.enabled", matchIfMissing = true)
	static class MongoConnectionPoolMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener(MeterRegistry meterRegistry,
				MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider) {
			return new MongoMetricsConnectionPoolListener(meterRegistry, mongoConnectionPoolTagsProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		MongoConnectionPoolTagsProvider mongoConnectionPoolTagsProvider() {
			return new DefaultMongoConnectionPoolTagsProvider();
		}

		@Bean
		MongoClientSettingsBuilderCustomizer mongoMetricsConnectionPoolListenerClientSettingsBuilderCustomizer(
				MongoMetricsConnectionPoolListener mongoMetricsConnectionPoolListener) {
			return (clientSettingsBuilder) -> clientSettingsBuilder
				.applyToConnectionPoolSettings((connectionPoolSettingsBuilder) -> connectionPoolSettingsBuilder
					.addConnectionPoolListener(mongoMetricsConnectionPoolListener));
		}

	}

}
