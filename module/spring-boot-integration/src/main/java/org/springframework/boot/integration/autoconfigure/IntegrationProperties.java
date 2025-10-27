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

package org.springframework.boot.integration.autoconfigure;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Integration.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @since 4.0.0
 */
@ConfigurationProperties("spring.integration")
public class IntegrationProperties {

	private final Channel channel = new Channel();

	private final Endpoint endpoint = new Endpoint();

	private final Error error = new Error();

	private final RSocket rsocket = new RSocket();

	private final Poller poller = new Poller();

	private final Management management = new Management();

	public Channel getChannel() {
		return this.channel;
	}

	public Endpoint getEndpoint() {
		return this.endpoint;
	}

	public Error getError() {
		return this.error;
	}

	public RSocket getRsocket() {
		return this.rsocket;
	}

	public Poller getPoller() {
		return this.poller;
	}

	public Management getManagement() {
		return this.management;
	}

	public static class Channel {

		/**
		 * Whether to create input channels if necessary.
		 */
		private boolean autoCreate = true;

		/**
		 * Default number of subscribers allowed on, for example, a 'DirectChannel'.
		 */
		private int maxUnicastSubscribers = Integer.MAX_VALUE;

		/**
		 * Default number of subscribers allowed on, for example, a
		 * 'PublishSubscribeChannel'.
		 */
		private int maxBroadcastSubscribers = Integer.MAX_VALUE;

		public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}

		public boolean isAutoCreate() {
			return this.autoCreate;
		}

		public void setMaxUnicastSubscribers(int maxUnicastSubscribers) {
			this.maxUnicastSubscribers = maxUnicastSubscribers;
		}

		public int getMaxUnicastSubscribers() {
			return this.maxUnicastSubscribers;
		}

		public void setMaxBroadcastSubscribers(int maxBroadcastSubscribers) {
			this.maxBroadcastSubscribers = maxBroadcastSubscribers;
		}

		public int getMaxBroadcastSubscribers() {
			return this.maxBroadcastSubscribers;
		}

	}

	public static class Endpoint {

		/**
		 * Whether to throw an exception when a reply is not expected anymore by a
		 * gateway.
		 */
		private boolean throwExceptionOnLateReply = false;

		/**
		 * List of message header names that should not be populated into Message
		 * instances during a header copying operation.
		 */
		private List<String> readOnlyHeaders = new ArrayList<>();

		/**
		 * List of endpoint bean names patterns that should not be started automatically
		 * during application startup.
		 */
		private List<String> noAutoStartup = new ArrayList<>();

		/**
		 * Default timeout for blocking operations such as sending or receiving messages.
		 */
		private Duration defaultTimeout = Duration.ofSeconds(30);

		public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
			this.throwExceptionOnLateReply = throwExceptionOnLateReply;
		}

		public boolean isThrowExceptionOnLateReply() {
			return this.throwExceptionOnLateReply;
		}

		public List<String> getReadOnlyHeaders() {
			return this.readOnlyHeaders;
		}

		public void setReadOnlyHeaders(List<String> readOnlyHeaders) {
			this.readOnlyHeaders = readOnlyHeaders;
		}

		public List<String> getNoAutoStartup() {
			return this.noAutoStartup;
		}

		public void setNoAutoStartup(List<String> noAutoStartup) {
			this.noAutoStartup = noAutoStartup;
		}

		public Duration getDefaultTimeout() {
			return this.defaultTimeout;
		}

		public void setDefaultTimeout(Duration defaultTimeout) {
			this.defaultTimeout = defaultTimeout;
		}

	}

	public static class Error {

		/**
		 * Whether to not silently ignore messages on the global 'errorChannel' when there
		 * are no subscribers.
		 */
		private boolean requireSubscribers = true;

		/**
		 * Whether to ignore failures for one or more of the handlers of the global
		 * 'errorChannel'.
		 */
		private boolean ignoreFailures = true;

		public boolean isRequireSubscribers() {
			return this.requireSubscribers;
		}

		public void setRequireSubscribers(boolean requireSubscribers) {
			this.requireSubscribers = requireSubscribers;
		}

		public boolean isIgnoreFailures() {
			return this.ignoreFailures;
		}

		public void setIgnoreFailures(boolean ignoreFailures) {
			this.ignoreFailures = ignoreFailures;
		}

	}

	public static class RSocket {

		private final Client client = new Client();

		private final Server server = new Server();

		public Client getClient() {
			return this.client;
		}

		public Server getServer() {
			return this.server;
		}

		public static class Client {

			/**
			 * TCP RSocket server host to connect to.
			 */
			private @Nullable String host;

			/**
			 * TCP RSocket server port to connect to.
			 */
			private @Nullable Integer port;

			/**
			 * WebSocket RSocket server uri to connect to.
			 */
			private @Nullable URI uri;

			public void setHost(@Nullable String host) {
				this.host = host;
			}

			public @Nullable String getHost() {
				return this.host;
			}

			public void setPort(@Nullable Integer port) {
				this.port = port;
			}

			public @Nullable Integer getPort() {
				return this.port;
			}

			public void setUri(@Nullable URI uri) {
				this.uri = uri;
			}

			public @Nullable URI getUri() {
				return this.uri;
			}

		}

		public static class Server {

			/**
			 * Whether to handle message mapping for RSocket through Spring Integration.
			 */
			private boolean messageMappingEnabled;

			public boolean isMessageMappingEnabled() {
				return this.messageMappingEnabled;
			}

			public void setMessageMappingEnabled(boolean messageMappingEnabled) {
				this.messageMappingEnabled = messageMappingEnabled;
			}

		}

	}

	public static class Poller {

		/**
		 * Maximum number of messages to poll per polling cycle.
		 */
		private int maxMessagesPerPoll = Integer.MIN_VALUE; // PollerMetadata.MAX_MESSAGES_UNBOUNDED

		/**
		 * How long to wait for messages on poll.
		 */
		private Duration receiveTimeout = Duration.ofSeconds(1); // PollerMetadata.DEFAULT_RECEIVE_TIMEOUT

		/**
		 * Polling delay period. Mutually exclusive with 'cron' and 'fixedRate'.
		 */
		private @Nullable Duration fixedDelay;

		/**
		 * Polling rate period. Mutually exclusive with 'fixedDelay' and 'cron'.
		 */
		private @Nullable Duration fixedRate;

		/**
		 * Polling initial delay. Applied for 'fixedDelay' and 'fixedRate'; ignored for
		 * 'cron'.
		 */
		private @Nullable Duration initialDelay;

		/**
		 * Cron expression for polling. Mutually exclusive with 'fixedDelay' and
		 * 'fixedRate'.
		 */
		private @Nullable String cron;

		public int getMaxMessagesPerPoll() {
			return this.maxMessagesPerPoll;
		}

		public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
			this.maxMessagesPerPoll = maxMessagesPerPoll;
		}

		public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public @Nullable Duration getFixedDelay() {
			return this.fixedDelay;
		}

		public void setFixedDelay(@Nullable Duration fixedDelay) {
			this.fixedDelay = fixedDelay;
		}

		public @Nullable Duration getFixedRate() {
			return this.fixedRate;
		}

		public void setFixedRate(@Nullable Duration fixedRate) {
			this.fixedRate = fixedRate;
		}

		public @Nullable Duration getInitialDelay() {
			return this.initialDelay;
		}

		public void setInitialDelay(@Nullable Duration initialDelay) {
			this.initialDelay = initialDelay;
		}

		public @Nullable String getCron() {
			return this.cron;
		}

		public void setCron(@Nullable String cron) {
			this.cron = cron;
		}

	}

	public static class Management {

		/**
		 * Whether Spring Integration components should perform logging in the main
		 * message flow. When disabled, such logging will be skipped without checking the
		 * logging level. When enabled, such logging is controlled as normal by the
		 * logging system's log level configuration.
		 */
		private boolean defaultLoggingEnabled = true;

		/**
		 * List of simple patterns to match against the names of Spring Integration
		 * components. When matched, observation instrumentation will be performed for the
		 * component. Please refer to the javadoc of the smartMatch method of Spring
		 * Integration's PatternMatchUtils for details of the pattern syntax.
		 */
		private List<String> observationPatterns = new ArrayList<>();

		public boolean isDefaultLoggingEnabled() {
			return this.defaultLoggingEnabled;
		}

		public void setDefaultLoggingEnabled(boolean defaultLoggingEnabled) {
			this.defaultLoggingEnabled = defaultLoggingEnabled;
		}

		public List<String> getObservationPatterns() {
			return this.observationPatterns;
		}

		public void setObservationPatterns(List<String> observationPatterns) {
			this.observationPatterns = observationPatterns;
		}

	}

}
