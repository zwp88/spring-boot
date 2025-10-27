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

package org.springframework.boot.mongodb.autoconfigure.health;

import com.mongodb.reactivestreams.client.MongoClient;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.boot.mongodb.health.MongoReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link MongoReactiveHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = MongoReactiveAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, Flux.class, MongoReactiveHealthIndicator.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(MongoClient.class)
@ConditionalOnEnabledHealthIndicator("mongodb")
public final class MongoReactiveHealthContributorAutoConfiguration
		extends CompositeReactiveHealthContributorConfiguration<MongoReactiveHealthIndicator, MongoClient> {

	MongoReactiveHealthContributorAutoConfiguration() {
		super(MongoReactiveHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "mongoHealthIndicator", "mongoHealthContributor" })
	ReactiveHealthContributor mongoHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, MongoClient.class);
	}

}
