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

package org.springframework.boot.opentelemetry.autoconfigure.logging;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for exporting logs using OpenTelemetry.
 *
 * @author Jonatan Ivanov
 * @since 4.0.0
 */
@ConfigurationProperties("management.opentelemetry.logging.export")
public class OpenTelemetryLoggingExportProperties {

	/**
	 * URL to the OTel collector's HTTP API.
	 */
	private @Nullable String endpoint;

	/**
	 * Call timeout for the OTel Collector to process an exported batch of data. This
	 * timeout spans the entire call: resolving DNS, connecting, writing the request body,
	 * server processing, and reading the response body. If the call requires redirects or
	 * retries all must complete within one timeout period.
	 */
	private Duration timeout = Duration.ofSeconds(10);

	/**
	 * Connect timeout for the OTel collector connection.
	 */
	private Duration connectTimeout = Duration.ofSeconds(10);

	/**
	 * Transport used to send the logs.
	 */
	private Transport transport = Transport.HTTP;

	/**
	 * Method used to compress the payload.
	 */
	private Compression compression = Compression.NONE;

	/**
	 * Custom HTTP headers you want to pass to the collector, for example auth headers.
	 */
	private final Map<String, String> headers = new HashMap<>();

	public @Nullable String getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(@Nullable String endpoint) {
		this.endpoint = endpoint;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Transport getTransport() {
		return this.transport;
	}

	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	public Compression getCompression() {
		return this.compression;
	}

	public void setCompression(Compression compression) {
		this.compression = compression;
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}

	public enum Compression {

		/**
		 * Gzip compression.
		 */
		GZIP,

		/**
		 * No compression.
		 */
		NONE

	}

}
