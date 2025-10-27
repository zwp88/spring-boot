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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.net.InetAddress;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.lang.Contract;
import org.springframework.util.StringUtils;

/**
 * Properties for the management server (e.g. port and path settings).
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Moritz Halbritter
 * @since 2.0.0
 * @see ServerProperties
 */
@ConfigurationProperties("management.server")
public class ManagementServerProperties {

	/**
	 * Management endpoint HTTP port (uses the same port as the application by default).
	 * Configure a different port to use management-specific SSL.
	 */
	private @Nullable Integer port;

	/**
	 * Network address to which the management endpoints should bind. Requires a custom
	 * management.server.port.
	 */
	private @Nullable InetAddress address;

	/**
	 * Management endpoint base path (for instance, '/management'). Requires a custom
	 * management.server.port.
	 */
	private String basePath = "";

	@NestedConfigurationProperty
	private @Nullable Ssl ssl;

	/**
	 * Returns the management port or {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used.
	 * @return the port
	 * @see #setPort(Integer)
	 */
	public @Nullable Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the management server, use {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used. Set to 0 to use a
	 * random port or set to -1 to disable.
	 * @param port the port
	 */
	public void setPort(@Nullable Integer port) {
		this.port = port;
	}

	public @Nullable InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(@Nullable InetAddress address) {
		this.address = address;
	}

	public String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = cleanBasePath(basePath);
	}

	public @Nullable Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(@Nullable Ssl ssl) {
		this.ssl = ssl;
	}

	@Contract("!null -> !null")
	private @Nullable String cleanBasePath(@Nullable String basePath) {
		String candidate = null;
		if (StringUtils.hasLength(basePath)) {
			candidate = basePath.strip();
		}
		if (StringUtils.hasText(candidate)) {
			if (!candidate.startsWith("/")) {
				candidate = "/" + candidate;
			}
			if (candidate.endsWith("/")) {
				candidate = candidate.substring(0, candidate.length() - 1);
			}
		}
		return candidate;
	}

}
