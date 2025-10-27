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

package org.springframework.boot.r2dbc.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.metrics.ConnectionPoolMetrics;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ConnectionFactory R2DBC connection factories}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ ConnectionPool.class, MeterRegistry.class })
@ConditionalOnBean({ ConnectionFactory.class, MeterRegistry.class })
public final class ConnectionPoolMetricsAutoConfiguration {

	@Autowired
	void bindConnectionPoolsToRegistry(ConfigurableListableBeanFactory beanFactory, MeterRegistry registry) {
		SimpleAutowireCandidateResolver.resolveAutowireCandidates(beanFactory, ConnectionFactory.class)
			.forEach((beanName, connectionFactory) -> {
				ConnectionPool pool = extractPool(connectionFactory);
				if (pool != null) {
					new ConnectionPoolMetrics(pool, beanName, Tags.empty()).bindTo(registry);
				}
			});
	}

	private @Nullable ConnectionPool extractPool(Object candidate) {
		if (candidate instanceof ConnectionPool connectionPool) {
			return connectionPool;
		}
		if (candidate instanceof Wrapped) {
			return extractPool(((Wrapped<?>) candidate).unwrap());
		}
		return null;
	}

}
