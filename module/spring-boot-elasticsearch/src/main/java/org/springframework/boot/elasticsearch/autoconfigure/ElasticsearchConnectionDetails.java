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

package org.springframework.boot.elasticsearch.autoconfigure;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to an Elasticsearch service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface ElasticsearchConnectionDetails extends ConnectionDetails {

	/**
	 * List of the Elasticsearch nodes to use.
	 * @return list of the Elasticsearch nodes to use
	 */
	List<Node> getNodes();

	/**
	 * Username for authentication with Elasticsearch.
	 * @return username for authentication with Elasticsearch or {@code null}
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Password for authentication with Elasticsearch.
	 * @return password for authentication with Elasticsearch or {@code null}
	 */
	default @Nullable String getPassword() {
		return null;
	}

	/**
	 * APIKey for authentication with Elasticsearch.
	 * @return the API key for authentication with Elasticsearch or {@code null}
	 */
	default @Nullable String getApiKey() {
		return null;
	}

	/**
	 * Prefix added to the path of every request sent to Elasticsearch.
	 * @return prefix added to the path of every request sent to Elasticsearch or
	 * {@code null}
	 */
	default @Nullable String getPathPrefix() {
		return null;
	}

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

	/**
	 * An Elasticsearch node.
	 *
	 * @param hostname the hostname
	 * @param port the port
	 * @param protocol the protocol
	 * @param username the username or {@code null}
	 * @param password the password or {@code null}
	 */
	record Node(String hostname, int port, Node.Protocol protocol, @Nullable String username,
			@Nullable String password) {

		public Node(String host, int port, Node.Protocol protocol) {
			this(host, port, protocol, null, null);
		}

		URI toUri() {
			try {
				return new URI(this.protocol.getScheme(), userInfo(), this.hostname, this.port, null, null, null);
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Can't construct URI", ex);
			}
		}

		private @Nullable String userInfo() {
			if (this.username == null) {
				return null;
			}
			return (this.password != null) ? (this.username + ":" + this.password) : this.username;
		}

		/**
		 * Connection protocol.
		 */
		public enum Protocol {

			/**
			 * HTTP.
			 */
			HTTP("http"),

			/**
			 * HTTPS.
			 */
			HTTPS("https");

			private final String scheme;

			Protocol(String scheme) {
				this.scheme = scheme;
			}

			String getScheme() {
				return this.scheme;
			}

			static Protocol forScheme(String scheme) {
				for (Protocol protocol : values()) {
					if (protocol.scheme.equals(scheme)) {
						return protocol;
					}
				}
				throw new IllegalArgumentException("Unknown scheme '" + scheme + "'");
			}

		}

	}

}
