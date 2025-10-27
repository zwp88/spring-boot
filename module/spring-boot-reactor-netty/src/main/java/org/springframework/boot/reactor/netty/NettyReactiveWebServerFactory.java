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

package org.springframework.boot.reactor.netty;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link NettyWebServer}s.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @author Scott Frederick
 * @since 4.0.0
 */
public class NettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private Set<NettyServerCustomizer> serverCustomizers = new LinkedHashSet<>();

	private final List<NettyRouteProvider> routeProviders = new ArrayList<>();

	private @Nullable Duration lifecycleTimeout;

	private boolean useForwardHeaders;

	private @Nullable ReactorResourceFactory resourceFactory;

	public NettyReactiveWebServerFactory() {
	}

	public NettyReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		HttpServer httpServer = createHttpServer();
		ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
		NettyWebServer webServer = createNettyWebServer(httpServer, handlerAdapter, this.lifecycleTimeout,
				getShutdown());
		webServer.setRouteProviders(this.routeProviders);
		return webServer;
	}

	NettyWebServer createNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
			@Nullable Duration lifecycleTimeout, Shutdown shutdown) {
		return new NettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown, this.resourceFactory);
	}

	/**
	 * Returns a mutable collection of the {@link NettyServerCustomizer}s that will be
	 * applied to the Netty server builder.
	 * @return the customizers that will be applied
	 */
	public Collection<NettyServerCustomizer> getServerCustomizers() {
		return this.serverCustomizers;
	}

	/**
	 * Set {@link NettyServerCustomizer}s that should be applied to the Netty server
	 * builder. Calling this method will replace any existing customizers.
	 * @param serverCustomizers the customizers to set
	 */
	public void setServerCustomizers(Collection<? extends NettyServerCustomizer> serverCustomizers) {
		Assert.notNull(serverCustomizers, "'serverCustomizers' must not be null");
		this.serverCustomizers = new LinkedHashSet<>(serverCustomizers);
	}

	/**
	 * Add {@link NettyServerCustomizer}s that should be applied while building the
	 * server.
	 * @param serverCustomizers the customizers to add
	 */
	public void addServerCustomizers(NettyServerCustomizer... serverCustomizers) {
		Assert.notNull(serverCustomizers, "'serverCustomizers' must not be null");
		this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
	}

	/**
	 * Add {@link NettyRouteProvider}s that should be applied, in order, before the
	 * handler for the Spring application.
	 * @param routeProviders the route providers to add
	 */
	public void addRouteProviders(NettyRouteProvider... routeProviders) {
		Assert.notNull(routeProviders, "'routeProviders' must not be null");
		this.routeProviders.addAll(Arrays.asList(routeProviders));
	}

	/**
	 * Set the maximum amount of time that should be waited when starting or stopping the
	 * server.
	 * @param lifecycleTimeout the lifecycle timeout
	 */
	public void setLifecycleTimeout(@Nullable Duration lifecycleTimeout) {
		this.lifecycleTimeout = lifecycleTimeout;
	}

	/**
	 * Set if x-forward-* headers should be processed.
	 * @param useForwardHeaders if x-forward headers should be used
	 */
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	/**
	 * Set the {@link ReactorResourceFactory} to get the shared resources from.
	 * @param resourceFactory the server resources
	 */
	public void setResourceFactory(@Nullable ReactorResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	private HttpServer createHttpServer() {
		HttpServer server = HttpServer.create().bindAddress(this::getListenAddress);
		Ssl ssl = getSsl();
		if (Ssl.isEnabled(ssl)) {
			server = customizeSslConfiguration(server, ssl);
		}
		if (getCompression() != null && getCompression().getEnabled()) {
			CompressionCustomizer compressionCustomizer = new CompressionCustomizer(getCompression());
			server = compressionCustomizer.apply(server);
		}
		server = server.protocol(listProtocols()).forwarded(this.useForwardHeaders);
		return applyCustomizers(server);
	}

	private HttpServer customizeSslConfiguration(HttpServer httpServer, Ssl ssl) {
		SslServerCustomizer customizer = new SslServerCustomizer(getHttp2(), ssl.getClientAuth(), getSslBundle(),
				getServerNameSslBundles());
		addBundleUpdateHandler(null, ssl.getBundle(), customizer);
		ssl.getServerNameBundles()
			.forEach((serverNameSslBundle) -> addBundleUpdateHandler(serverNameSslBundle.serverName(),
					serverNameSslBundle.bundle(), customizer));
		return customizer.apply(httpServer);
	}

	private void addBundleUpdateHandler(@Nullable String serverName, @Nullable String bundleName,
			SslServerCustomizer customizer) {
		if (StringUtils.hasText(bundleName)) {
			SslBundles sslBundles = getSslBundles();
			Assert.state(sslBundles != null, "'sslBundles' must not be null");
			sslBundles.addBundleUpdateHandler(bundleName,
					(sslBundle) -> customizer.updateSslBundle(serverName, sslBundle));
		}
	}

	private HttpProtocol[] listProtocols() {
		List<HttpProtocol> protocols = new ArrayList<>();
		protocols.add(HttpProtocol.HTTP11);
		if (getHttp2() != null && getHttp2().isEnabled()) {
			if (getSsl() != null && getSsl().isEnabled()) {
				protocols.add(HttpProtocol.H2);
			}
			else {
				protocols.add(HttpProtocol.H2C);
			}
		}
		return protocols.toArray(new HttpProtocol[0]);
	}

	private InetSocketAddress getListenAddress() {
		if (getAddress() != null) {
			return new InetSocketAddress(getAddress().getHostAddress(), getPort());
		}
		return new InetSocketAddress(getPort());
	}

	private HttpServer applyCustomizers(HttpServer server) {
		for (NettyServerCustomizer customizer : this.serverCustomizers) {
			server = customizer.apply(server);
		}
		return server;
	}

}
