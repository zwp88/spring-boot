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

package org.springframework.boot.mail.autoconfigure;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for email support.
 *
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@ConfigurationProperties("spring.mail")
public class MailProperties {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * SMTP server host. For instance, 'smtp.example.com'.
	 */
	private @Nullable String host;

	/**
	 * SMTP server port.
	 */
	private @Nullable Integer port;

	/**
	 * Login user of the SMTP server.
	 */
	private @Nullable String username;

	/**
	 * Login password of the SMTP server.
	 */
	private @Nullable String password;

	/**
	 * Protocol used by the SMTP server.
	 */
	private String protocol = "smtp";

	/**
	 * Default MimeMessage encoding.
	 */
	private Charset defaultEncoding = DEFAULT_CHARSET;

	/**
	 * Additional JavaMail Session properties.
	 */
	private final Map<String, String> properties = new HashMap<>();

	/**
	 * Session JNDI name. When set, takes precedence over other Session settings.
	 */
	private @Nullable String jndiName;

	/**
	 * SSL configuration.
	 */
	private final Ssl ssl = new Ssl();

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

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Charset getDefaultEncoding() {
		return this.defaultEncoding;
	}

	public void setDefaultEncoding(Charset defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public @Nullable String getJndiName() {
		return this.jndiName;
	}

	public void setJndiName(@Nullable String jndiName) {
		this.jndiName = jndiName;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support. If enabled, 'mail.(protocol).ssl.enable'
		 * property is set to 'true'.
		 */
		private boolean enabled = false;

		/**
		 * SSL bundle name. If set, 'mail.(protocol).ssl.socketFactory' property is set to
		 * an SSLSocketFactory obtained from the corresponding SSL bundle.
		 * <p>
		 * Note that the STARTTLS command can use the corresponding SSLSocketFactory, even
		 * if the 'mail.(protocol).ssl.enable' property is not set.
		 */
		private @Nullable String bundle;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

}
