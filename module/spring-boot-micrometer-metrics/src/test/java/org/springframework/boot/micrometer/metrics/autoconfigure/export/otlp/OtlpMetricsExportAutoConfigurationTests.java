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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp;

import java.util.concurrent.ScheduledExecutorService;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration.PropertiesOtlpMetricsConnectionDetails;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.assertj.ScheduledExecutorServiceAssert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtlpMetricsExportAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class OtlpMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OtlpMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class));
	}

	@Test
	void autoConfiguresConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class)
				.hasSingleBean(OtlpConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class)
				.doesNotHaveBean(OtlpConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.otlp.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class)
				.doesNotHaveBean(OtlpConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class)
				.hasSingleBean(OtlpConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsPlatformThreadsToBeUsed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(OtlpMeterRegistry.class);
			OtlpMeterRegistry registry = context.getBean(OtlpMeterRegistry.class);
			assertThat(registry).extracting("scheduledExecutorService")
				.satisfies((executor) -> ScheduledExecutorServiceAssert.assertThat((ScheduledExecutorService) executor)
					.usesPlatformThreads());
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void allowsVirtualThreadsToBeUsed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("spring.threads.virtual.enabled=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpMeterRegistry.class);
				OtlpMeterRegistry registry = context.getBean(OtlpMeterRegistry.class);
				assertThat(registry).extracting("scheduledExecutorService")
					.satisfies(
							(executor) -> ScheduledExecutorServiceAssert.assertThat((ScheduledExecutorService) executor)
								.usesVirtualThreads());
			});
	}

	@Test
	void allowsRegistryToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class)
				.hasSingleBean(OtlpConfig.class)
				.hasBean("customRegistry"));
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(PropertiesOtlpMetricsConnectionDetails.class));
	}

	@Test
	void testConnectionFactoryWithOverridesWhenUsingCustomConnectionDetails() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class, ConnectionDetailsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class)
					.doesNotHaveBean(PropertiesOtlpMetricsConnectionDetails.class);
				OtlpConfig config = context.getBean(OtlpConfig.class);
				assertThat(config.url()).isEqualTo("http://localhost:12345/v1/metrics");
			});
	}

	@Test
	void allowsCustomMetricsSenderToBeUsed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class, CustomMetricsSenderConfiguration.class)
			.run(this::assertHasCustomMetricsSender);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void allowsCustomMetricsSenderToBeUsedWithVirtualThreads() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class, CustomMetricsSenderConfiguration.class)
			.withPropertyValues("spring.threads.virtual.enabled=true")
			.run(this::assertHasCustomMetricsSender);
	}

	@Test
	void shouldBackOffIfSpringBootOpenTelemetryIsMissing() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withClassLoader(new FilteredClassLoader("org.springframework.boot.opentelemetry"))
			.run((context) -> assertThat(context).doesNotHaveBean(OtlpMetricsExportAutoConfiguration.class));
	}

	private void assertHasCustomMetricsSender(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(OtlpMeterRegistry.class);
		OtlpMeterRegistry registry = context.getBean(OtlpMeterRegistry.class);
		assertThat(registry).extracting("metricsSender")
			.satisfies((sender) -> assertThat(sender).isSameAs(CustomMetricsSenderConfiguration.customMetricsSender));
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		Clock customClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		OtlpConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		OtlpMeterRegistry customRegistry(OtlpConfig config, Clock clock) {
			return new OtlpMeterRegistry(config, clock);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsConfiguration {

		@Bean
		OtlpMetricsConnectionDetails otlpConnectionDetails() {
			return () -> "http://localhost:12345/v1/metrics";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMetricsSenderConfiguration {

		static OtlpMetricsSender customMetricsSender = (request) -> {
		};

		@Bean
		OtlpMetricsSender customMetricsSender() {
			return customMetricsSender;
		}

	}

}
