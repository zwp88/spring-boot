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

package org.springframework.boot.health.autoconfigure.contributor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.PingHealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for default {@link HealthContributor
 * health contributors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
public final class HealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnEnabledHealthIndicator("ping")
	PingHealthIndicator pingHealthContributor() {
		return new PingHealthIndicator();
	}

}
