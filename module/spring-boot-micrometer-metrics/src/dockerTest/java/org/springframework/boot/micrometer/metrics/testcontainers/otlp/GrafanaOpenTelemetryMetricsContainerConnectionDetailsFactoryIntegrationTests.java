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

package org.springframework.boot.micrometer.metrics.testcontainers.otlp;

import java.net.URI;
import java.time.Duration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Tests for {@link GrafanaOpenTelemetryMetricsContainerConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@TestPropertySource(properties = { "management.opentelemetry.resource-attributes.service.name=test",
		"management.otlp.metrics.export.step=1s" })
@Testcontainers(disabledWithoutDocker = true)
class GrafanaOpenTelemetryMetricsContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final LgtmStackContainer container = TestImage.container(LgtmStackContainer.class);

	@Autowired
	private MeterRegistry meterRegistry;

	@Test
	void connectionCanBeMadeToOpenTelemetryCollectorContainer() {
		Counter.builder("test.counter").register(this.meterRegistry).increment(42);
		Gauge.builder("test.gauge", () -> 12).register(this.meterRegistry);
		Timer.builder("test.timer").register(this.meterRegistry).record(Duration.ofMillis(123));
		DistributionSummary.builder("test.distributionsummary").register(this.meterRegistry).record(24);
		Awaitility.given()
			.pollInterval(Duration.ofSeconds(2))
			.atMost(Duration.ofSeconds(10))
			.ignoreExceptions()
			.untilAsserted(() -> {
				RestTestClient restClient = RestTestClient.bindToServer().build();
				restClient.get()
					.uri(URI.create(container.getPrometheusHttpUrl() + "/api/v1/query?query=%7Bjob=%22test%22%7D"))
					.exchange()
					.expectStatus()
					.isOk()
					.expectBody()
					.jsonPath(metricWithValue("test_counter_total", "42"))
					.exists()
					.jsonPath(metricWithValue("test_timer_milliseconds_count", "1"))
					.exists()
					.jsonPath(metricWithValue("test_timer_milliseconds_sum", "123"))
					.exists()
					.jsonPath(metricWithValue("test_timer_milliseconds_bucket", "1"))
					.exists()
					.jsonPath(metricWithValue("test_distributionsummary_count", "1"))
					.exists()
					.jsonPath(metricWithValue("test_distributionsummary_sum", "24"))
					.exists()
					.jsonPath(metricWithValue("test_distributionsummary_bucket", "1"))
					.exists();
			});
	}

	private String metricWithValue(String metric, String value) {
		return "$.data.result[?(@.metric.__name__==\"%s\" && \"%s\" in @.value)]".formatted(metric, value);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(OtlpMetricsExportAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		Clock customClock() {
			return Clock.SYSTEM;
		}

	}

}
