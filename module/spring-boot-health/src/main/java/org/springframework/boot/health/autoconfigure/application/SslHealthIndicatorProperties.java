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

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.health.application.SslHealthIndicator;

/**
 * External configuration properties for {@link SslHealthIndicator}.
 *
 * @author Jonatan Ivanov
 * @since 4.0.0
 */
@ConfigurationProperties("management.health.ssl")
public class SslHealthIndicatorProperties {

	/**
	 * If an SSL Certificate will be invalid within the time span defined by this
	 * threshold, it should trigger a warning.
	 */
	private Duration certificateValidityWarningThreshold = Duration.ofDays(14);

	public Duration getCertificateValidityWarningThreshold() {
		return this.certificateValidityWarningThreshold;
	}

	public void setCertificateValidityWarningThreshold(Duration certificateValidityWarningThreshold) {
		this.certificateValidityWarningThreshold = certificateValidityWarningThreshold;
	}

}
