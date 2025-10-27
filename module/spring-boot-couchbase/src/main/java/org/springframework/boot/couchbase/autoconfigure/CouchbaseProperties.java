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

package org.springframework.boot.couchbase.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Yulin Qin
 * @author Brian Clozel
 * @author Michael Nitschinger
 * @author Scott Frederick
 * @since 4.0.0
 */
@ConfigurationProperties("spring.couchbase")
public class CouchbaseProperties {

	/**
	 * Connection string used to locate the Couchbase cluster.
	 */
	private @Nullable String connectionString;

	/**
	 * Cluster username.
	 */
	private @Nullable String username;

	/**
	 * Cluster password.
	 */
	private @Nullable String password;

	private final Authentication authentication = new Authentication();

	private final Env env = new Env();

	public @Nullable String getConnectionString() {
		return this.connectionString;
	}

	public void setConnectionString(@Nullable String connectionString) {
		this.connectionString = connectionString;
	}

	public @Nullable String getUsername() {
		return this.username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public Authentication getAuthentication() {
		return this.authentication;
	}

	public Env getEnv() {
		return this.env;
	}

	public static class Authentication {

		private final Pem pem = new Pem();

		private final Jks jks = new Jks();

		public Pem getPem() {
			return this.pem;
		}

		public Jks getJks() {
			return this.jks;
		}

		public static class Pem {

			/**
			 * PEM-formatted certificates for certificate-based cluster authentication.
			 */
			private @Nullable String certificates;

			/**
			 * PEM-formatted private key for certificate-based cluster authentication.
			 */
			private @Nullable String privateKey;

			/**
			 * Private key password for certificate-based cluster authentication.
			 */
			private @Nullable String privateKeyPassword;

			public @Nullable String getCertificates() {
				return this.certificates;
			}

			public void setCertificates(@Nullable String certificates) {
				this.certificates = certificates;
			}

			public @Nullable String getPrivateKey() {
				return this.privateKey;
			}

			public void setPrivateKey(@Nullable String privateKey) {
				this.privateKey = privateKey;
			}

			public @Nullable String getPrivateKeyPassword() {
				return this.privateKeyPassword;
			}

			public void setPrivateKeyPassword(@Nullable String privateKeyPassword) {
				this.privateKeyPassword = privateKeyPassword;
			}

		}

		public static class Jks {

			/**
			 * Java KeyStore location for certificate-based cluster authentication.
			 */
			private @Nullable String location;

			/**
			 * Java KeyStore password for certificate-based cluster authentication.
			 */
			private @Nullable String password;

			public @Nullable String getLocation() {
				return this.location;
			}

			public void setLocation(@Nullable String location) {
				this.location = location;
			}

			public @Nullable String getPassword() {
				return this.password;
			}

			public void setPassword(@Nullable String password) {
				this.password = password;
			}

		}

	}

	public static class Env {

		private final Io io = new Io();

		private final Ssl ssl = new Ssl();

		private final Timeouts timeouts = new Timeouts();

		public Io getIo() {
			return this.io;
		}

		public Ssl getSsl() {
			return this.ssl;
		}

		public Timeouts getTimeouts() {
			return this.timeouts;
		}

	}

	public static class Io {

		/**
		 * Minimum number of sockets per node.
		 */
		private int minEndpoints = 1;

		/**
		 * Maximum number of sockets per node.
		 */
		private int maxEndpoints = 12;

		/**
		 * Length of time an HTTP connection may remain idle before it is closed and
		 * removed from the pool.
		 */
		private Duration idleHttpConnectionTimeout = Duration.ofSeconds(1);

		public int getMinEndpoints() {
			return this.minEndpoints;
		}

		public void setMinEndpoints(int minEndpoints) {
			this.minEndpoints = minEndpoints;
		}

		public int getMaxEndpoints() {
			return this.maxEndpoints;
		}

		public void setMaxEndpoints(int maxEndpoints) {
			this.maxEndpoints = maxEndpoints;
		}

		public Duration getIdleHttpConnectionTimeout() {
			return this.idleHttpConnectionTimeout;
		}

		public void setIdleHttpConnectionTimeout(Duration idleHttpConnectionTimeout) {
			this.idleHttpConnectionTimeout = idleHttpConnectionTimeout;
		}

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if a "bundle" is provided
		 * unless specified otherwise.
		 */
		private @Nullable Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		public Boolean getEnabled() {
			return (this.enabled != null) ? this.enabled : StringUtils.hasText(this.bundle);
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

	public static class Timeouts {

		/**
		 * Bucket connect timeout.
		 */
		private Duration connect = Duration.ofSeconds(10);

		/**
		 * Bucket disconnect timeout.
		 */
		private Duration disconnect = Duration.ofSeconds(10);

		/**
		 * Timeout for operations on a specific key-value.
		 */
		private Duration keyValue = Duration.ofMillis(2500);

		/**
		 * Timeout for operations on a specific key-value with a durability level.
		 */
		private Duration keyValueDurable = Duration.ofSeconds(10);

		/**
		 * N1QL query operations timeout.
		 */
		private Duration query = Duration.ofSeconds(75);

		/**
		 * Regular and geospatial view operations timeout.
		 */
		private Duration view = Duration.ofSeconds(75);

		/**
		 * Timeout for the search service.
		 */
		private Duration search = Duration.ofSeconds(75);

		/**
		 * Timeout for the analytics service.
		 */
		private Duration analytics = Duration.ofSeconds(75);

		/**
		 * Timeout for the management operations.
		 */
		private Duration management = Duration.ofSeconds(75);

		public Duration getConnect() {
			return this.connect;
		}

		public void setConnect(Duration connect) {
			this.connect = connect;
		}

		public Duration getDisconnect() {
			return this.disconnect;
		}

		public void setDisconnect(Duration disconnect) {
			this.disconnect = disconnect;
		}

		public Duration getKeyValue() {
			return this.keyValue;
		}

		public void setKeyValue(Duration keyValue) {
			this.keyValue = keyValue;
		}

		public Duration getKeyValueDurable() {
			return this.keyValueDurable;
		}

		public void setKeyValueDurable(Duration keyValueDurable) {
			this.keyValueDurable = keyValueDurable;
		}

		public Duration getQuery() {
			return this.query;
		}

		public void setQuery(Duration query) {
			this.query = query;
		}

		public Duration getView() {
			return this.view;
		}

		public void setView(Duration view) {
			this.view = view;
		}

		public Duration getSearch() {
			return this.search;
		}

		public void setSearch(Duration search) {
			this.search = search;
		}

		public Duration getAnalytics() {
			return this.analytics;
		}

		public void setAnalytics(Duration analytics) {
			this.analytics = analytics;
		}

		public Duration getManagement() {
			return this.management;
		}

		public void setManagement(Duration management) {
			this.management = management;
		}

	}

}
