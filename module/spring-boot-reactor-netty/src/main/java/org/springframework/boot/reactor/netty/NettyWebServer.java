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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.unix.Errors.NativeIoException;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.netty.ChannelBindException;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.resources.LoopResources;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * {@link WebServer} that can be used to control a Reactor Netty web server. Usually this
 * class should be created using the {@link NettyReactiveWebServerFactory} and not
 * directly.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class NettyWebServer implements WebServer {

	/**
	 * Permission denied error code from {@code errno.h}.
	 */
	private static final int ERROR_NO_EACCES = -13;

	private static final Predicate<HttpServerRequest> ALWAYS = (request) -> true;

	private static final Log logger = LogFactory.getLog(NettyWebServer.class);

	private final HttpServer httpServer;

	private final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler;

	private final @Nullable Duration lifecycleTimeout;

	private final @Nullable GracefulShutdown gracefulShutdown;

	private final @Nullable ReactorResourceFactory resourceFactory;

	private List<NettyRouteProvider> routeProviders = Collections.emptyList();

	private volatile @Nullable DisposableServer disposableServer;

	/**
	 * Creates a new {@code NettyWebServer} instance.
	 * @param httpServer the HTTP server
	 * @param handlerAdapter the handler adapter
	 * @param lifecycleTimeout the lifecycle timeout, may be {@code null}
	 * @param shutdown the shutdown, may be {@code null}
	 * @param resourceFactory the factory for the server's {@link LoopResources loop
	 * resources}, may be {@code null}
	 * @since 4.0.0
	 */
	public NettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
			@Nullable Duration lifecycleTimeout, @Nullable Shutdown shutdown,
			@Nullable ReactorResourceFactory resourceFactory) {
		Assert.notNull(httpServer, "'httpServer' must not be null");
		Assert.notNull(handlerAdapter, "'handlerAdapter' must not be null");
		this.lifecycleTimeout = lifecycleTimeout;
		this.handler = handlerAdapter;
		this.httpServer = httpServer.channelGroup(new DefaultChannelGroup(new DefaultEventExecutor()));
		this.gracefulShutdown = (shutdown == Shutdown.GRACEFUL) ? new GracefulShutdown(() -> this.disposableServer)
				: null;
		this.resourceFactory = resourceFactory;
	}

	public void setRouteProviders(List<NettyRouteProvider> routeProviders) {
		this.routeProviders = routeProviders;
	}

	@Override
	public void start() throws WebServerException {
		DisposableServer disposableServer = this.disposableServer;
		if (disposableServer == null) {
			try {
				disposableServer = startHttpServer();
				this.disposableServer = disposableServer;
			}
			catch (Exception ex) {
				PortInUseException.ifCausedBy(ex, ChannelBindException.class, (bindException) -> {
					if (bindException.localPort() > 0 && !isPermissionDenied(bindException.getCause())) {
						throw new PortInUseException(bindException.localPort(), ex);
					}
				});
				throw new WebServerException("Unable to start Netty", ex);
			}
			logger.info(getStartedOnMessage(disposableServer));
			startDaemonAwaitThread(disposableServer);
		}
	}

	private String getStartedOnMessage(DisposableServer server) {
		StringBuilder message = new StringBuilder();
		tryAppend(message, "port %s", () -> server.port()
				+ ((this.httpServer.configuration().sslProvider() != null) ? " (https)" : " (http)"));
		tryAppend(message, "path %s", server::path);
		return (!message.isEmpty()) ? "Netty started on " + message : "Netty started";
	}

	protected String getStartedLogMessage() {
		DisposableServer disposableServer = this.disposableServer;
		Assert.state(disposableServer != null, "'disposableServer' must not be null");
		return getStartedOnMessage(disposableServer);
	}

	private void tryAppend(StringBuilder message, String format, Supplier<Object> supplier) {
		try {
			Object value = supplier.get();
			message.append((!message.isEmpty()) ? " " : "");
			message.append(String.format(format, value));
		}
		catch (UnsupportedOperationException ex) {
			// Ignore
		}
	}

	DisposableServer startHttpServer() {
		HttpServer server = this.httpServer;
		if (this.routeProviders.isEmpty()) {
			server = server.handle(this.handler);
		}
		else {
			server = server.route(this::applyRouteProviders);
		}
		if (this.resourceFactory != null) {
			LoopResources resources = this.resourceFactory.getLoopResources();
			Assert.state(resources != null, "No LoopResources: is ReactorResourceFactory not initialized yet?");
			server = server.runOn(resources);
		}
		if (this.lifecycleTimeout != null) {
			return server.bindNow(this.lifecycleTimeout);
		}
		return server.bindNow();
	}

	private boolean isPermissionDenied(@Nullable Throwable bindExceptionCause) {
		try {
			if (bindExceptionCause instanceof NativeIoException nativeException) {
				return nativeException.expectedErr() == ERROR_NO_EACCES;
			}
		}
		catch (Throwable ignore) {
		}
		return false;
	}

	/**
	 * Initiates a graceful shutdown of the Netty web server. Handling of new requests is
	 * prevented and the given {@code callback} is invoked at the end of the attempt. The
	 * attempt can be explicitly ended by invoking {@link #stop}.
	 * <p>
	 * Once shutdown has been initiated Netty will reject any new connections. Requests +
	 * on existing idle connections will also be rejected.
	 */
	@Override
	public void shutDownGracefully(GracefulShutdownCallback callback) {
		if (this.gracefulShutdown == null) {
			callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
			return;
		}
		this.gracefulShutdown.shutDownGracefully(callback);
	}

	private void applyRouteProviders(HttpServerRoutes routes) {
		for (NettyRouteProvider provider : this.routeProviders) {
			routes = provider.apply(routes);
		}
		routes.route(ALWAYS, this.handler);
	}

	private void startDaemonAwaitThread(DisposableServer disposableServer) {
		Thread awaitThread = new Thread("server") {

			@Override
			public void run() {
				disposableServer.onDispose().block();
			}

		};
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	@Override
	public void stop() throws WebServerException {
		DisposableServer disposableServer = this.disposableServer;
		if (disposableServer != null) {
			if (this.gracefulShutdown != null) {
				this.gracefulShutdown.abort();
			}
			try {
				if (this.lifecycleTimeout != null) {
					disposableServer.disposeNow(this.lifecycleTimeout);
				}
				else {
					disposableServer.disposeNow();
				}
			}
			catch (IllegalStateException ex) {
				// Continue
			}
			this.disposableServer = null;
		}
	}

	@Override
	public int getPort() {
		DisposableServer disposableServer = this.disposableServer;
		if (disposableServer != null) {
			try {
				return disposableServer.port();
			}
			catch (UnsupportedOperationException ex) {
				return -1;
			}
		}
		return -1;
	}

}
