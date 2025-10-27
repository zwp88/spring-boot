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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.stackdriver;

import io.micrometer.core.instrument.Clock;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to
 * Stackdriver.
 *
 * @author Johannes Graf
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(StackdriverMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("stackdriver")
@EnableConfigurationProperties(StackdriverProperties.class)
public final class StackdriverMetricsExportAutoConfiguration {

	private final StackdriverProperties properties;

	StackdriverMetricsExportAutoConfiguration(StackdriverProperties stackdriverProperties) {
		this.properties = stackdriverProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	StackdriverConfig stackdriverConfig() {
		return new StackdriverPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	StackdriverMeterRegistry stackdriverMeterRegistry(StackdriverConfig stackdriverConfig, Clock clock) {
		return StackdriverMeterRegistry.builder(stackdriverConfig).clock(clock).build();
	}

}
