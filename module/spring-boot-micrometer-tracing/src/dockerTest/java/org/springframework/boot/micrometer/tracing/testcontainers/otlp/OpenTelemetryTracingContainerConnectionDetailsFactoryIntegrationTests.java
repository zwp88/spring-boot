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

package org.springframework.boot.micrometer.tracing.testcontainers.otlp;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.OtlpTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.Transport;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryTracingContainerConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class OpenTelemetryTracingContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final GenericContainer<?> container = TestImage.OPENTELEMETRY.genericContainer()
		.withExposedPorts(4317, 4318);

	@Autowired
	private OtlpTracingConnectionDetails connectionDetails;

	@Test
	void connectionCanBeMadeToOpenTelemetryContainer() {
		assertThat(this.connectionDetails.getUrl(Transport.HTTP))
			.isEqualTo("http://" + container.getHost() + ":" + container.getMappedPort(4318) + "/v1/traces");
		assertThat(this.connectionDetails.getUrl(Transport.GRPC))
			.isEqualTo("http://" + container.getHost() + ":" + container.getMappedPort(4317) + "/v1/traces");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(OtlpTracingAutoConfiguration.class)
	static class TestConfiguration {

	}

}
