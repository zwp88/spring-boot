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

package org.springframework.boot.autoconfigure.ssl;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;

/**
 * Base class for SSL Bundle properties.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 * @see SslBundle
 */
public abstract class SslBundleProperties {

	/**
	 * Key details for the bundle.
	 */
	private final Key key = new Key();

	/**
	 * Options for the SSL connection.
	 */
	private final Options options = new Options();

	/**
	 * SSL Protocol to use.
	 */
	private String protocol = SslBundle.DEFAULT_PROTOCOL;

	/**
	 * Whether to reload the SSL bundle.
	 */
	private boolean reloadOnUpdate;

	public Key getKey() {
		return this.key;
	}

	public Options getOptions() {
		return this.options;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public boolean isReloadOnUpdate() {
		return this.reloadOnUpdate;
	}

	public void setReloadOnUpdate(boolean reloadOnUpdate) {
		this.reloadOnUpdate = reloadOnUpdate;
	}

	public static class Options {

		/**
		 * Supported SSL ciphers.
		 */
		private @Nullable Set<String> ciphers;

		/**
		 * Enabled SSL protocols.
		 */
		private @Nullable Set<String> enabledProtocols;

		public @Nullable Set<String> getCiphers() {
			return this.ciphers;
		}

		public void setCiphers(@Nullable Set<String> ciphers) {
			this.ciphers = ciphers;
		}

		public @Nullable Set<String> getEnabledProtocols() {
			return this.enabledProtocols;
		}

		public void setEnabledProtocols(@Nullable Set<String> enabledProtocols) {
			this.enabledProtocols = enabledProtocols;
		}

	}

	public static class Key {

		/**
		 * The password used to access the key in the key store.
		 */
		private @Nullable String password;

		/**
		 * The alias that identifies the key in the key store.
		 */
		private @Nullable String alias;

		public @Nullable String getPassword() {
			return this.password;
		}

		public void setPassword(@Nullable String password) {
			this.password = password;
		}

		public @Nullable String getAlias() {
			return this.alias;
		}

		public void setAlias(@Nullable String alias) {
			this.alias = alias;
		}

	}

}
