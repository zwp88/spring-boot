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

package org.springframework.boot.amqp.autoconfigure.metrics;

import com.rabbitmq.client.ConnectionFactory;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ConnectionFactory connection factories}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
		after = RabbitAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, AbstractConnectionFactory.class, MeterRegistry.class })
@ConditionalOnBean({ org.springframework.amqp.rabbit.connection.ConnectionFactory.class, MeterRegistry.class })
public final class RabbitMetricsAutoConfiguration {

	@Bean
	static RabbitConnectionFactoryMetricsPostProcessor rabbitConnectionFactoryMetricsPostProcessor(
			ApplicationContext applicationContext) {
		return new RabbitConnectionFactoryMetricsPostProcessor(applicationContext);
	}

}
