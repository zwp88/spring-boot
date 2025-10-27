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

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Handles Jetty graceful shutdown.
 *
 * @author Andy Wilkinson
 * @author Onur Kagan Ozcan
 */
final class GracefulShutdown {

	private static final Log logger = LogFactory.getLog(GracefulShutdown.class);

	private final Server server;

	private final Supplier<Integer> activeRequests;

	private volatile boolean aborted = false;

	GracefulShutdown(Server server, Supplier<Integer> activeRequests) {
		this.server = server;
		this.activeRequests = activeRequests;
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		new Thread(() -> awaitShutdown(callback), "jetty-shutdown").start();
		boolean jetty10 = isJetty10();
		for (Connector connector : this.server.getConnectors()) {
			shutdown(connector, !jetty10);
		}

	}

	@SuppressWarnings("unchecked")
	private void shutdown(Connector connector, boolean getResult) {
		Future<Void> result;
		try {
			result = connector.shutdown();
		}
		catch (NoSuchMethodError ex) {
			Method shutdown = ReflectionUtils.findMethod(connector.getClass(), "shutdown");
			Assert.state(shutdown != null, "'shutdown' must not be null");
			result = (Future<Void>) ReflectionUtils.invokeMethod(shutdown, connector);
		}
		if (getResult) {
			try {
				Assert.state(result != null, "'result' must not be null");
				result.get();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (ExecutionException ex) {
				// Continue
			}
		}
	}

	private boolean isJetty10() {
		try {
			return CompletableFuture.class.equals(Connector.class.getMethod("shutdown").getReturnType());
		}
		catch (Exception ex) {
			return false;
		}
	}

	private void awaitShutdown(GracefulShutdownCallback callback) {
		while (!this.aborted && this.activeRequests.get() > 0) {
			sleep(100);
		}
		if (this.aborted) {
			logger.info("Graceful shutdown aborted with one or more requests still active");
			callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
		}
		else {
			logger.info("Graceful shutdown complete");
			callback.shutdownComplete(GracefulShutdownResult.IDLE);
		}
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	void abort() {
		this.aborted = true;
	}

}
