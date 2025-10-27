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

package org.springframework.boot.web.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.server.Ssl.ServerNameSslBundle;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link ConfigurableWebServerFactory} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Ivan Sopov
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 2.0.0
 */
public abstract class AbstractConfigurableWebServerFactory implements ConfigurableWebServerFactory {

	private int port = 8080;

	private @Nullable InetAddress address;

	private Set<ErrorPage> errorPages = new LinkedHashSet<>();

	private @Nullable Ssl ssl;

	private @Nullable SslBundles sslBundles;

	private @Nullable Http2 http2;

	private @Nullable Compression compression;

	private @Nullable String serverHeader;

	private Shutdown shutdown = Shutdown.IMMEDIATE;

	/**
	 * Create a new {@link AbstractConfigurableWebServerFactory} instance.
	 */
	public AbstractConfigurableWebServerFactory() {
	}

	/**
	 * Create a new {@link AbstractConfigurableWebServerFactory} instance with the
	 * specified port.
	 * @param port the port number for the web server
	 */
	public AbstractConfigurableWebServerFactory(int port) {
		this.port = port;
	}

	/**
	 * The port that the web server listens on.
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Return the address that the web server binds to.
	 * @return the address
	 */
	public @Nullable InetAddress getAddress() {
		return this.address;
	}

	@Override
	public void setAddress(@Nullable InetAddress address) {
		this.address = address;
	}

	/**
	 * Returns a mutable set of {@link ErrorPage ErrorPages} that will be used when
	 * handling exceptions.
	 * @return the error pages
	 */
	public Set<ErrorPage> getErrorPages() {
		return this.errorPages;
	}

	@Override
	public void setErrorPages(Set<? extends ErrorPage> errorPages) {
		Assert.notNull(errorPages, "'errorPages' must not be null");
		this.errorPages = new LinkedHashSet<>(errorPages);
	}

	@Override
	public void addErrorPages(ErrorPage... errorPages) {
		Assert.notNull(errorPages, "'errorPages' must not be null");
		this.errorPages.addAll(Arrays.asList(errorPages));
	}

	public @Nullable Ssl getSsl() {
		return this.ssl;
	}

	@Override
	public void setSsl(@Nullable Ssl ssl) {
		this.ssl = ssl;
	}

	/**
	 * Return the configured {@link SslBundles}.
	 * @return the {@link SslBundles} or {@code null}
	 * @since 3.2.0
	 */
	public @Nullable SslBundles getSslBundles() {
		return this.sslBundles;
	}

	@Override
	public void setSslBundles(@Nullable SslBundles sslBundles) {
		this.sslBundles = sslBundles;
	}

	public @Nullable Http2 getHttp2() {
		return this.http2;
	}

	@Override
	public void setHttp2(@Nullable Http2 http2) {
		this.http2 = http2;
	}

	public @Nullable Compression getCompression() {
		return this.compression;
	}

	@Override
	public void setCompression(@Nullable Compression compression) {
		this.compression = compression;
	}

	public @Nullable String getServerHeader() {
		return this.serverHeader;
	}

	@Override
	public void setServerHeader(@Nullable String serverHeader) {
		this.serverHeader = serverHeader;
	}

	@Override
	public void setShutdown(Shutdown shutdown) {
		this.shutdown = shutdown;
	}

	/**
	 * Returns the shutdown configuration that will be applied to the server.
	 * @return the shutdown configuration
	 * @since 2.3.0
	 */
	public Shutdown getShutdown() {
		return this.shutdown;
	}

	/**
	 * Return the {@link SslBundle} that should be used with this server.
	 * @return the SSL bundle
	 */
	protected final SslBundle getSslBundle() {
		return WebServerSslBundle.get(this.ssl, this.sslBundles);
	}

	protected final Map<String, SslBundle> getServerNameSslBundles() {
		Assert.state(this.ssl != null, "'ssl' must not be null");
		return this.ssl.getServerNameBundles()
			.stream()
			.collect(Collectors.toMap(ServerNameSslBundle::serverName, (serverNameSslBundle) -> {
				Assert.state(this.sslBundles != null, "'sslBundles' must not be null");
				return this.sslBundles.getBundle(serverNameSslBundle.bundle());
			}));
	}

	/**
	 * Return the absolute temp dir for given web server.
	 * @param prefix server name
	 * @return the temp dir for given server.
	 */
	protected final File createTempDir(String prefix) {
		try {
			File tempDir = Files.createTempDirectory(prefix + "." + getPort() + ".").toFile();
			tempDir.deleteOnExit();
			return tempDir;
		}
		catch (IOException ex) {
			throw new WebServerException(
					"Unable to create tempDir. java.io.tmpdir is set to " + System.getProperty("java.io.tmpdir"), ex);
		}
	}

}
