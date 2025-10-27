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

package org.springframework.boot.rsocket.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.netty.handler.ssl.ClientAuth;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.SslProvider.GenericSslContextSpec;
import reactor.netty.tcp.SslProvider.SslContextSpec;
import reactor.netty.tcp.TcpServer;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.rsocket.server.ConfigurableRSocketServerFactory;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ServerNameSslBundle;
import org.springframework.boot.web.server.WebServerSslBundle;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;

/**
 * {@link RSocketServerFactory} that can be used to create {@link RSocketServer}s backed
 * by Netty.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @author Scott Frederick
 * @since 2.2.0
 */
public class NettyRSocketServerFactory implements RSocketServerFactory, ConfigurableRSocketServerFactory {

	private int port = 9898;

	private @Nullable DataSize fragmentSize;

	private @Nullable InetAddress address;

	private RSocketServer.Transport transport = RSocketServer.Transport.TCP;

	private @Nullable ReactorResourceFactory resourceFactory;

	private @Nullable Duration lifecycleTimeout;

	private List<RSocketServerCustomizer> rSocketServerCustomizers = new ArrayList<>();

	private @Nullable Ssl ssl;

	private @Nullable SslBundles sslBundles;

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void setFragmentSize(@Nullable DataSize fragmentSize) {
		this.fragmentSize = fragmentSize;
	}

	@Override
	public void setAddress(@Nullable InetAddress address) {
		this.address = address;
	}

	@Override
	public void setTransport(RSocketServer.Transport transport) {
		this.transport = transport;
	}

	@Override
	public void setSsl(@Nullable Ssl ssl) {
		this.ssl = ssl;
	}

	@Override
	public void setSslBundles(@Nullable SslBundles sslBundles) {
		this.sslBundles = sslBundles;
	}

	/**
	 * Set the {@link ReactorResourceFactory} to get the shared resources from.
	 * @param resourceFactory the server resources
	 */
	public void setResourceFactory(@Nullable ReactorResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}

	/**
	 * Set {@link RSocketServerCustomizer}s that should be called to configure the
	 * {@link io.rsocket.core.RSocketServer} while building the server. Calling this
	 * method will replace any existing customizers.
	 * @param rSocketServerCustomizers customizers to apply before the server starts
	 * @since 2.2.7
	 */
	public void setRSocketServerCustomizers(Collection<? extends RSocketServerCustomizer> rSocketServerCustomizers) {
		Assert.notNull(rSocketServerCustomizers, "'rSocketServerCustomizers' must not be null");
		this.rSocketServerCustomizers = new ArrayList<>(rSocketServerCustomizers);
	}

	/**
	 * Add {@link RSocketServerCustomizer}s that should be called to configure the
	 * {@link io.rsocket.core.RSocketServer}.
	 * @param rSocketServerCustomizers customizers to apply before the server starts
	 * @since 2.2.7
	 */
	public void addRSocketServerCustomizers(RSocketServerCustomizer... rSocketServerCustomizers) {
		Assert.notNull(rSocketServerCustomizers, "'rSocketServerCustomizers' must not be null");
		this.rSocketServerCustomizers.addAll(Arrays.asList(rSocketServerCustomizers));
	}

	/**
	 * Set the maximum amount of time that should be waited when starting or stopping the
	 * server.
	 * @param lifecycleTimeout the lifecycle timeout
	 */
	public void setLifecycleTimeout(Duration lifecycleTimeout) {
		this.lifecycleTimeout = lifecycleTimeout;
	}

	@Override
	public NettyRSocketServer create(SocketAcceptor socketAcceptor) {
		ServerTransport<CloseableChannel> transport = createTransport();
		io.rsocket.core.RSocketServer server = io.rsocket.core.RSocketServer.create(socketAcceptor);
		configureServer(server);
		Mono<CloseableChannel> starter = server.bind(transport);
		return new NettyRSocketServer(starter, this.lifecycleTimeout);
	}

	private void configureServer(io.rsocket.core.RSocketServer server) {
		PropertyMapper map = PropertyMapper.get();
		map.from(this.fragmentSize).asInt(DataSize::toBytes).to(server::fragment);
		this.rSocketServerCustomizers.forEach((customizer) -> customizer.customize(server));
	}

	private ServerTransport<CloseableChannel> createTransport() {
		if (this.transport == RSocketServer.Transport.WEBSOCKET) {
			return createWebSocketTransport();
		}
		return createTcpTransport();
	}

	private ServerTransport<CloseableChannel> createWebSocketTransport() {
		HttpServer httpServer = HttpServer.create();
		if (this.resourceFactory != null) {
			httpServer = httpServer.runOn(this.resourceFactory.getLoopResources());
		}
		if (Ssl.isEnabled(this.ssl)) {
			httpServer = customizeSslConfiguration(httpServer, this.ssl);
		}
		return WebsocketServerTransport.create(httpServer.bindAddress(this::getListenAddress));
	}

	private HttpServer customizeSslConfiguration(HttpServer httpServer, Ssl ssl) {
		return new HttpServerSslCustomizer(ssl.getClientAuth(), getSslBundle(), getServerNameSslBundles())
			.apply(httpServer);
	}

	private ServerTransport<CloseableChannel> createTcpTransport() {
		TcpServer tcpServer = TcpServer.create();
		if (this.resourceFactory != null) {
			tcpServer = tcpServer.runOn(this.resourceFactory.getLoopResources());
		}
		if (Ssl.isEnabled(this.ssl)) {
			tcpServer = new TcpServerSslCustomizer(this.ssl.getClientAuth(), getSslBundle(), getServerNameSslBundles())
				.apply(tcpServer);
		}
		return TcpServerTransport.create(tcpServer.bindAddress(this::getListenAddress));
	}

	private SslBundle getSslBundle() {
		return WebServerSslBundle.get(this.ssl, this.sslBundles);
	}

	protected final Map<String, SslBundle> getServerNameSslBundles() {
		Assert.state(this.ssl != null, "'ssl' must not be null");
		return this.ssl.getServerNameBundles()
			.stream()
			.collect(Collectors.toMap(Ssl.ServerNameSslBundle::serverName, this::getBundle));
	}

	private SslBundle getBundle(ServerNameSslBundle serverNameSslBundle) {
		Assert.state(this.sslBundles != null, "'sslBundles' must not be null");
		return this.sslBundles.getBundle(serverNameSslBundle.bundle());
	}

	private InetSocketAddress getListenAddress() {
		if (this.address != null) {
			return new InetSocketAddress(this.address.getHostAddress(), this.port);
		}
		return new InetSocketAddress(this.port);
	}

	private abstract static class SslCustomizer {

		private final ClientAuth clientAuth;

		protected SslCustomizer(ClientAuth clientAuth) {
			this.clientAuth = clientAuth;
		}

		protected final AbstractProtocolSslContextSpec<?> createSslContextSpec(SslBundle sslBundle) {
			AbstractProtocolSslContextSpec<?> sslContextSpec = Http11SslContextSpec
				.forServer(sslBundle.getManagers().getKeyManagerFactory());
			return sslContextSpec.configure((builder) -> {
				builder.trustManager(sslBundle.getManagers().getTrustManagerFactory());
				SslOptions options = sslBundle.getOptions();
				builder.protocols(options.getEnabledProtocols());
				builder.ciphers(SslOptions.asSet(options.getCiphers()));
				builder.clientAuth(this.clientAuth);
			});
		}

	}

	private static final class TcpServerSslCustomizer extends SslCustomizer {

		private final SslBundle sslBundle;

		private TcpServerSslCustomizer(Ssl.@Nullable ClientAuth clientAuth, SslBundle sslBundle,
				Map<String, SslBundle> serverNameSslBundles) {
			super(Ssl.ClientAuth.map(clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE));
			this.sslBundle = sslBundle;
		}

		private TcpServer apply(TcpServer server) {
			GenericSslContextSpec<?> sslContextSpec = createSslContextSpec(this.sslBundle);
			return server.secure((spec) -> spec.sslContext(sslContextSpec));
		}

	}

	private static final class HttpServerSslCustomizer extends SslCustomizer {

		private final SslProvider sslProvider;

		private final Map<String, SslProvider> serverNameSslProviders;

		private HttpServerSslCustomizer(Ssl.@Nullable ClientAuth clientAuth, SslBundle sslBundle,
				Map<String, SslBundle> serverNameSslBundles) {
			super(Ssl.ClientAuth.map(clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE));
			this.sslProvider = createSslProvider(sslBundle);
			this.serverNameSslProviders = createServerNameSslProviders(serverNameSslBundles);
		}

		private HttpServer apply(HttpServer server) {
			return server.secure(this::applySecurity);
		}

		private void applySecurity(SslContextSpec spec) {
			spec.sslContext(this.sslProvider.getSslContext()).setSniAsyncMappings((serverName, promise) -> {
				SslProvider provider = (serverName != null) ? this.serverNameSslProviders.get(serverName)
						: this.sslProvider;
				return promise.setSuccess(provider);
			});
		}

		private Map<String, SslProvider> createServerNameSslProviders(Map<String, SslBundle> serverNameSslBundles) {
			Map<String, SslProvider> serverNameSslProviders = new HashMap<>();
			serverNameSslBundles.forEach(
					(serverName, sslBundle) -> serverNameSslProviders.put(serverName, createSslProvider(sslBundle)));
			return serverNameSslProviders;
		}

		private SslProvider createSslProvider(SslBundle sslBundle) {
			return SslProvider.builder().sslContext((GenericSslContextSpec<?>) createSslContextSpec(sslBundle)).build();
		}

	}

}
