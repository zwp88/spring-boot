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

package org.springframework.boot.health.autoconfigure.application;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.application.SslHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link SslHealthIndicator}.
 *
 * @author Jonatan Ivanov
 * @since 4.0.0
 */
@AutoConfiguration(before = HealthContributorAutoConfiguration.class)
@ConditionalOnClass(Health.class)
@ConditionalOnEnabledHealthIndicator("ssl")
@EnableConfigurationProperties(SslHealthIndicatorProperties.class)
public final class SslHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "sslHealthIndicator")
	SslHealthIndicator sslHealthIndicator(SslInfo sslInfo, SslHealthIndicatorProperties properties) {
		return new SslHealthIndicator(sslInfo, properties.getCertificateValidityWarningThreshold());
	}

	@Bean
	@ConditionalOnMissingBean
	SslInfo sslInfo(SslBundles sslBundles) {
		return new SslInfo(sslBundles);
	}

}
