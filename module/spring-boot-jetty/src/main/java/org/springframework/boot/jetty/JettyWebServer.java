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

package org.springframework.boot.jetty;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.jetty.reactive.JettyReactiveWebServerFactory;
import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link WebServer} that can be used to control a Jetty web server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author David Liu
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author Kristine Jetzke
 * @since 4.0.0
 * @see JettyReactiveWebServerFactory
 */
public class JettyWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(JettyWebServer.class);

	private final Object monitor = new Object();

	private final Server server;

	private final boolean autoStart;

	private final @Nullable GracefulShutdown gracefulShutdown;

	private Connector[] connectors;

	private volatile boolean started;

	/**
	 * Create a new {@link JettyWebServer} instance.
	 * @param server the underlying Jetty server
	 */
	public JettyWebServer(Server server) {
		this(server, true);
	}

	/**
	 * Create a new {@link JettyWebServer} instance.
	 * @param server the underlying Jetty server
	 * @param autoStart if auto-starting the server
	 */
	public JettyWebServer(Server server, boolean autoStart) {
		this.autoStart = autoStart;
		Assert.notNull(server, "'server' must not be null");
		this.server = server;
		this.gracefulShutdown = createGracefulShutdown(server);
		initialize();
	}

	private @Nullable GracefulShutdown createGracefulShutdown(Server server) {
		StatisticsHandler statisticsHandler = findStatisticsHandler(server);
		if (statisticsHandler == null) {
			return null;
		}
		return new GracefulShutdown(server, statisticsHandler::getRequestsActive);
	}

	private @Nullable StatisticsHandler findStatisticsHandler(Server server) {
		return findStatisticsHandler(server.getHandler());
	}

	private @Nullable StatisticsHandler findStatisticsHandler(Handler handler) {
		if (handler instanceof StatisticsHandler statisticsHandler) {
			return statisticsHandler;
		}
		if (handler instanceof Handler.Wrapper handlerWrapper) {
			return findStatisticsHandler(handlerWrapper.getHandler());
		}
		return null;
	}

	private void initialize() {
		synchronized (this.monitor) {
			try {
				// Cache the connectors and then remove them to prevent requests being
				// handled before the application context is ready.
				this.connectors = this.server.getConnectors();
				JettyWebServer.this.server.setConnectors(null);
				// Start the server so that the ServletContext is available
				this.server.start();
				this.server.setStopAtShutdown(false);
			}
			catch (Throwable ex) {
				// Ensure process isn't left running
				stopSilently();
				throw new WebServerException("Unable to start embedded Jetty web server", ex);
			}
		}
	}

	private void stopSilently() {
		try {
			this.server.stop();
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	@Override
	public void start() throws WebServerException {
		synchronized (this.monitor) {
			if (this.started) {
				return;
			}
			this.server.setConnectors(this.connectors);
			if (!this.autoStart) {
				return;
			}
			try {
				this.server.start();
				handleDeferredInitialize(this.server);
				Connector[] connectors = this.server.getConnectors();
				for (Connector connector : connectors) {
					try {
						connector.start();
					}
					catch (IOException ex) {
						if (connector instanceof NetworkConnector networkConnector) {
							PortInUseException.throwIfPortBindingException(ex, networkConnector::getPort);
						}
						throw ex;
					}
				}
				this.started = true;
				logger.info(getStartedLogMessage());
			}
			catch (WebServerException ex) {
				stopSilently();
				throw ex;
			}
			catch (Exception ex) {
				stopSilently();
				throw new WebServerException("Unable to start embedded Jetty server", ex);
			}
		}
	}

	String getStartedLogMessage() {
		String contextPath = getContextPath();
		return "Jetty started on " + getActualPortsDescription()
				+ ((contextPath != null) ? " with context path '" + contextPath + "'" : "");
	}

	private String getActualPortsDescription() {
		StringBuilder description = new StringBuilder("port");
		Connector[] connectors = this.server.getConnectors();
		if (connectors.length != 1) {
			description.append("s");
		}
		description.append(" ");
		for (int i = 0; i < connectors.length; i++) {
			if (i != 0) {
				description.append(", ");
			}
			Connector connector = connectors[i];
			description.append(getLocalPort(connector)).append(getProtocols(connector));
		}
		return description.toString();
	}

	private String getProtocols(Connector connector) {
		List<String> protocols = connector.getProtocols();
		return " (" + StringUtils.collectionToDelimitedString(protocols, ", ") + ")";
	}

	private @Nullable String getContextPath() {
		List<ContextHandler> imperativeContextHandlers = this.server.getHandlers()
			.stream()
			.map(this::findContextHandler)
			.filter(Objects::nonNull)
			.filter(this::isImperative)
			.toList();
		if (imperativeContextHandlers.isEmpty()) {
			return null;
		}
		return imperativeContextHandlers.stream().map(ContextHandler::getContextPath).collect(Collectors.joining(" "));
	}

	private @Nullable ContextHandler findContextHandler(Handler handler) {
		while (handler instanceof Handler.Wrapper handlerWrapper) {
			if (handler instanceof ContextHandler contextHandler) {
				return contextHandler;
			}
			handler = handlerWrapper.getHandler();
		}
		return null;
	}

	private boolean isImperative(ContextHandler contextHandler) {
		if (contextHandler instanceof ServletContextHandler servletContextHandler) {
			Collection<ServletHolder> servletHolders = servletContextHandler.getServletHandler()
				.getBeans(ServletHolder.class);
			for (ServletHolder servletHolder : servletHolders) {
				if (ServletHttpHandlerAdapter.class.getName().equals(servletHolder.getClassName())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Performs any necessary handling of deferred initialization.
	 * @param server the server that has been started
	 * @throws Exception if a failure occurs during the deferred initialization
	 */
	protected void handleDeferredInitialize(Server server) throws Exception {

	}

	@Override
	public void stop() {
		synchronized (this.monitor) {
			this.started = false;
			if (this.gracefulShutdown != null) {
				this.gracefulShutdown.abort();
			}
			try {
				for (Connector connector : this.server.getConnectors()) {
					connector.stop();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to stop embedded Jetty server", ex);
			}
		}
	}

	@Override
	public void destroy() {
		synchronized (this.monitor) {
			try {
				this.server.stop();
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to destroy embedded Jetty server", ex);
			}
		}
	}

	@Override
	public int getPort() {
		Connector[] connectors = this.server.getConnectors();
		for (Connector connector : connectors) {
			int localPort = getLocalPort(connector);
			if (localPort > 0) {
				return localPort;
			}
		}
		return -1;
	}

	private int getLocalPort(Connector connector) {
		if (connector instanceof NetworkConnector networkConnector) {
			return networkConnector.getLocalPort();
		}
		return 0;
	}

	/**
	 * Initiates a graceful shutdown of the Jetty web server. Handling of new requests is
	 * prevented and the given {@code callback} is invoked at the end of the attempt. The
	 * attempt can be explicitly ended by invoking {@link #stop}.
	 * <p>
	 * Once shutdown has been initiated Jetty will reject any new connections. Requests on
	 * existing connections will be accepted, however, a {@code Connection: close} header
	 * will be returned in the response.
	 */
	@Override
	public void shutDownGracefully(GracefulShutdownCallback callback) {
		if (this.gracefulShutdown == null) {
			callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
			return;
		}
		this.gracefulShutdown.shutDownGracefully(callback);
	}

	/**
	 * Returns access to the underlying Jetty Server.
	 * @return the Jetty server
	 */
	public Server getServer() {
		return this.server;
	}

}
