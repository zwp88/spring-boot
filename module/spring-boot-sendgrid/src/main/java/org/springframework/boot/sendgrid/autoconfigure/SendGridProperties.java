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

package org.springframework.boot.sendgrid.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for SendGrid.
 *
 * @author Maciej Walkowiak
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.sendgrid")
public class SendGridProperties {

	/**
	 * SendGrid API key.
	 */
	private @Nullable String apiKey;

	/**
	 * Proxy configuration.
	 */
	private @Nullable Proxy proxy;

	public @Nullable String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(@Nullable String apiKey) {
		this.apiKey = apiKey;
	}

	public @Nullable Proxy getProxy() {
		return this.proxy;
	}

	public void setProxy(@Nullable Proxy proxy) {
		this.proxy = proxy;
	}

	public static class Proxy {

		/**
		 * SendGrid proxy host.
		 */
		private @Nullable String host;

		/**
		 * SendGrid proxy port.
		 */
		private @Nullable Integer port;

		public @Nullable String getHost() {
			return this.host;
		}

		public void setHost(@Nullable String host) {
			this.host = host;
		}

		public @Nullable Integer getPort() {
			return this.port;
		}

		public void setPort(@Nullable Integer port) {
			this.port = port;
		}

	}

}
