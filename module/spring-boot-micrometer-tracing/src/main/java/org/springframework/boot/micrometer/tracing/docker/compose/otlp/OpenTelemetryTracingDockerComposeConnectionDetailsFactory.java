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

package org.springframework.boot.micrometer.tracing.docker.compose.otlp;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.Transport;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpTracingConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class OpenTelemetryTracingDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OtlpTracingConnectionDetails> {

	private static final String[] OPENTELEMETRY_IMAGE_NAMES = { "otel/opentelemetry-collector-contrib",
			"grafana/otel-lgtm" };

	private static final int OTLP_GRPC_PORT = 4317;

	private static final int OTLP_HTTP_PORT = 4318;

	OpenTelemetryTracingDockerComposeConnectionDetailsFactory() {
		super(OPENTELEMETRY_IMAGE_NAMES,
				"org.springframework.boot.micrometer.tracing.autoconfigure.otlp.OtlpTracingAutoConfiguration");
	}

	@Override
	protected OtlpTracingConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OpenTelemetryTracingDockerComposeConnectionDetails(source.getRunningService());
	}

	private static final class OpenTelemetryTracingDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OtlpTracingConnectionDetails {

		private final String host;

		private final int grpcPort;

		private final int httpPort;

		private OpenTelemetryTracingDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.grpcPort = source.ports().get(OTLP_GRPC_PORT);
			this.httpPort = source.ports().get(OTLP_HTTP_PORT);
		}

		@Override
		public String getUrl(Transport transport) {
			int port = switch (transport) {
				case HTTP -> this.httpPort;
				case GRPC -> this.grpcPort;
			};
			return "http://%s:%d/v1/traces".formatted(this.host, port);
		}

	}

}
