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

package org.springframework.boot.amqp.health;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for the
 * RabbitMQ messaging system.
 *
 * @author Christian Dupuis
 * @since 4.0.0
 */
public class RabbitHealthIndicator extends AbstractHealthIndicator {

	private final RabbitTemplate rabbitTemplate;

	public RabbitHealthIndicator(RabbitTemplate rabbitTemplate) {
		super("Rabbit health check failed");
		Assert.notNull(rabbitTemplate, "'rabbitTemplate' must not be null");
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		builder.up();
		String version = getVersion();
		if (version != null) {
			builder.withDetail("version", version);
		}
	}

	private @Nullable String getVersion() {
		return this.rabbitTemplate.execute((channel) -> {
			Object version = channel.getConnection().getServerProperties().get("version");
			Assert.state(version != null, "'version' must not be null");
			return version.toString();
		});
	}

}
