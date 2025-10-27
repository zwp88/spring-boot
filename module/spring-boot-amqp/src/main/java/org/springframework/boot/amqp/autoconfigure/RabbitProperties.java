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

package org.springframework.boot.amqp.autoconfigure;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory.AddressShuffleMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Rabbit.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Josh Thornhill
 * @author Gary Russell
 * @author Artsiom Yudovin
 * @author Franjo Zilic
 * @author Eddú Meléndez
 * @author Rafael Carvalho
 * @author Scott Frederick
 * @author Lasse Wulff
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("spring.rabbitmq")
public class RabbitProperties {

	private static final int DEFAULT_PORT = 5672;

	private static final int DEFAULT_PORT_SECURE = 5671;

	private static final int DEFAULT_STREAM_PORT = 5552;

	/**
	 * RabbitMQ host. Ignored if an address is set.
	 */
	private String host = "localhost";

	/**
	 * RabbitMQ port. Ignored if an address is set. Default to 5672, or 5671 if SSL is
	 * enabled.
	 */
	private @Nullable Integer port;

	/**
	 * Login user to authenticate to the broker.
	 */
	private String username = "guest";

	/**
	 * Login to authenticate against the broker.
	 */
	private String password = "guest";

	/**
	 * SSL configuration.
	 */
	private final Ssl ssl = new Ssl();

	/**
	 * Virtual host to use when connecting to the broker.
	 */
	private @Nullable String virtualHost;

	/**
	 * List of addresses to which the client should connect. When set, the host and port
	 * are ignored.
	 */
	private @Nullable List<String> addresses;

	/**
	 * Mode used to shuffle configured addresses.
	 */
	private AddressShuffleMode addressShuffleMode = AddressShuffleMode.NONE;

	/**
	 * Requested heartbeat timeout; zero for none. If a duration suffix is not specified,
	 * seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private @Nullable Duration requestedHeartbeat;

	/**
	 * Number of channels per connection requested by the client. Use 0 for unlimited.
	 */
	private int requestedChannelMax = 2047;

	/**
	 * Whether to enable publisher returns.
	 */
	private boolean publisherReturns;

	/**
	 * Type of publisher confirms to use.
	 */
	private @Nullable ConfirmType publisherConfirmType;

	/**
	 * Connection timeout. Set it to zero to wait forever.
	 */
	private @Nullable Duration connectionTimeout;

	/**
	 * Continuation timeout for RPC calls in channels. Set it to zero to wait forever.
	 */
	private Duration channelRpcTimeout = Duration.ofMinutes(10);

	/**
	 * Maximum size of the body of inbound (received) messages.
	 */
	private DataSize maxInboundMessageBodySize = DataSize.ofMegabytes(64);

	/**
	 * Cache configuration.
	 */
	private final Cache cache = new Cache();

	/**
	 * Listener container configuration.
	 */
	private final Listener listener = new Listener();

	private final Template template = new Template();

	private final Stream stream = new Stream();

	private @Nullable List<Address> parsedAddresses;

	public String getHost() {
		return this.host;
	}

	/**
	 * Returns the host from the first address, or the configured host if no addresses
	 * have been set.
	 * @return the host
	 * @see #setAddresses(List)
	 * @see #getHost()
	 */
	public String determineHost() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return getHost();
		}
		return this.parsedAddresses.get(0).host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public @Nullable Integer getPort() {
		return this.port;
	}

	/**
	 * Returns the port from the first address, or the configured port if no addresses
	 * have been set.
	 * @return the port
	 * @see #setAddresses(List)
	 * @see #getPort()
	 */
	public int determinePort() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			Integer port = getPort();
			if (port != null) {
				return port;
			}
			return Boolean.TRUE.equals(getSsl().getEnabled()) ? DEFAULT_PORT_SECURE : DEFAULT_PORT;
		}
		return this.parsedAddresses.get(0).port;
	}

	public void setPort(@Nullable Integer port) {
		this.port = port;
	}

	public @Nullable List<String> getAddresses() {
		return this.addresses;
	}

	/**
	 * Returns the configured addresses or a single address ({@code host:port}) created
	 * from the configured host and port if no addresses have been set.
	 * @return the addresses
	 */
	public List<String> determineAddresses() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			if (this.host.contains(",")) {
				throw new InvalidConfigurationPropertyValueException("spring.rabbitmq.host", this.host,
						"Invalid character ','. Value must be a single host. For multiple hosts, use property 'spring.rabbitmq.addresses' instead.");
			}
			return List.of(this.host + ":" + determinePort());
		}
		List<String> addressStrings = new ArrayList<>();
		for (Address parsedAddress : this.parsedAddresses) {
			addressStrings.add(parsedAddress.host + ":" + parsedAddress.port);
		}
		return addressStrings;
	}

	public void setAddresses(List<String> addresses) {
		this.addresses = addresses;
		this.parsedAddresses = parseAddresses(addresses);
	}

	private List<Address> parseAddresses(List<String> addresses) {
		List<Address> parsedAddresses = new ArrayList<>();
		for (String address : addresses) {
			parsedAddresses.add(new Address(address, Boolean.TRUE.equals(getSsl().getEnabled())));
		}
		return parsedAddresses;
	}

	public String getUsername() {
		return this.username;
	}

	/**
	 * If addresses have been set and the first address has a username it is returned.
	 * Otherwise returns the result of calling {@code getUsername()}.
	 * @return the username
	 * @see #setAddresses(List)
	 * @see #getUsername()
	 */
	public String determineUsername() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return this.username;
		}
		Address address = this.parsedAddresses.get(0);
		return (address.username != null) ? address.username : this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	/**
	 * If addresses have been set and the first address has a password it is returned.
	 * Otherwise returns the result of calling {@code getPassword()}.
	 * @return the password or {@code null}
	 * @see #setAddresses(List)
	 * @see #getPassword()
	 */
	public @Nullable String determinePassword() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return getPassword();
		}
		Address address = this.parsedAddresses.get(0);
		return (address.password != null) ? address.password : getPassword();
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public @Nullable String getVirtualHost() {
		return this.virtualHost;
	}

	/**
	 * If addresses have been set and the first address has a virtual host it is returned.
	 * Otherwise returns the result of calling {@code getVirtualHost()}.
	 * @return the virtual host or {@code null}
	 * @see #setAddresses(List)
	 * @see #getVirtualHost()
	 */
	public @Nullable String determineVirtualHost() {
		if (CollectionUtils.isEmpty(this.parsedAddresses)) {
			return getVirtualHost();
		}
		Address address = this.parsedAddresses.get(0);
		return (address.virtualHost != null) ? address.virtualHost : getVirtualHost();
	}

	public void setVirtualHost(@Nullable String virtualHost) {
		this.virtualHost = StringUtils.hasText(virtualHost) ? virtualHost : "/";
	}

	public AddressShuffleMode getAddressShuffleMode() {
		return this.addressShuffleMode;
	}

	public void setAddressShuffleMode(AddressShuffleMode addressShuffleMode) {
		this.addressShuffleMode = addressShuffleMode;
	}

	public @Nullable Duration getRequestedHeartbeat() {
		return this.requestedHeartbeat;
	}

	public void setRequestedHeartbeat(@Nullable Duration requestedHeartbeat) {
		this.requestedHeartbeat = requestedHeartbeat;
	}

	public int getRequestedChannelMax() {
		return this.requestedChannelMax;
	}

	public void setRequestedChannelMax(int requestedChannelMax) {
		this.requestedChannelMax = requestedChannelMax;
	}

	public boolean isPublisherReturns() {
		return this.publisherReturns;
	}

	public void setPublisherReturns(boolean publisherReturns) {
		this.publisherReturns = publisherReturns;
	}

	public @Nullable Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setPublisherConfirmType(@Nullable ConfirmType publisherConfirmType) {
		this.publisherConfirmType = publisherConfirmType;
	}

	public @Nullable ConfirmType getPublisherConfirmType() {
		return this.publisherConfirmType;
	}

	public void setConnectionTimeout(@Nullable Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Duration getChannelRpcTimeout() {
		return this.channelRpcTimeout;
	}

	public void setChannelRpcTimeout(Duration channelRpcTimeout) {
		this.channelRpcTimeout = channelRpcTimeout;
	}

	public DataSize getMaxInboundMessageBodySize() {
		return this.maxInboundMessageBodySize;
	}

	public void setMaxInboundMessageBodySize(DataSize maxInboundMessageBodySize) {
		this.maxInboundMessageBodySize = maxInboundMessageBodySize;
	}

	public Cache getCache() {
		return this.cache;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Template getTemplate() {
		return this.template;
	}

	public Stream getStream() {
		return this.stream;
	}

	public class Ssl {

		private static final String SUN_X509 = "SunX509";

		/**
		 * Whether to enable SSL support. Determined automatically if an address is
		 * provided with the protocol (amqp:// vs. amqps://).
		 */
		private @Nullable Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		/**
		 * Path to the key store that holds the SSL certificate.
		 */
		private @Nullable String keyStore;

		/**
		 * Key store type.
		 */
		private String keyStoreType = "PKCS12";

		/**
		 * Password used to access the key store.
		 */
		private @Nullable String keyStorePassword;

		/**
		 * Key store algorithm.
		 */
		private String keyStoreAlgorithm = SUN_X509;

		/**
		 * Trust store that holds SSL certificates.
		 */
		private @Nullable String trustStore;

		/**
		 * Trust store type.
		 */
		private String trustStoreType = "JKS";

		/**
		 * Password used to access the trust store.
		 */
		private @Nullable String trustStorePassword;

		/**
		 * Trust store algorithm.
		 */
		private String trustStoreAlgorithm = SUN_X509;

		/**
		 * SSL algorithm to use. By default, configured by the Rabbit client library.
		 */
		private @Nullable String algorithm;

		/**
		 * Whether to enable server side certificate validation.
		 */
		private boolean validateServerCertificate = true;

		/**
		 * Whether to enable hostname verification.
		 */
		private boolean verifyHostname = true;

		public @Nullable Boolean getEnabled() {
			return this.enabled;
		}

		/**
		 * Returns whether SSL is enabled from the first address, or the configured ssl
		 * enabled flag if no addresses have been set.
		 * @return whether ssl is enabled
		 * @see #setAddresses(List)
		 * @see #getEnabled() ()
		 */
		public boolean determineEnabled() {
			boolean defaultEnabled = Boolean.TRUE.equals(getEnabled()) || this.bundle != null;
			if (CollectionUtils.isEmpty(RabbitProperties.this.parsedAddresses)) {
				return defaultEnabled;
			}
			Address address = RabbitProperties.this.parsedAddresses.get(0);
			return address.determineSslEnabled(defaultEnabled);
		}

		public void setEnabled(@Nullable Boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

		public @Nullable String getKeyStore() {
			return this.keyStore;
		}

		public void setKeyStore(@Nullable String keyStore) {
			this.keyStore = keyStore;
		}

		public String getKeyStoreType() {
			return this.keyStoreType;
		}

		public void setKeyStoreType(String keyStoreType) {
			this.keyStoreType = keyStoreType;
		}

		public @Nullable String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		public void setKeyStorePassword(@Nullable String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		public String getKeyStoreAlgorithm() {
			return this.keyStoreAlgorithm;
		}

		public void setKeyStoreAlgorithm(String keyStoreAlgorithm) {
			this.keyStoreAlgorithm = keyStoreAlgorithm;
		}

		public @Nullable String getTrustStore() {
			return this.trustStore;
		}

		public void setTrustStore(@Nullable String trustStore) {
			this.trustStore = trustStore;
		}

		public String getTrustStoreType() {
			return this.trustStoreType;
		}

		public void setTrustStoreType(String trustStoreType) {
			this.trustStoreType = trustStoreType;
		}

		public @Nullable String getTrustStorePassword() {
			return this.trustStorePassword;
		}

		public void setTrustStorePassword(@Nullable String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		public String getTrustStoreAlgorithm() {
			return this.trustStoreAlgorithm;
		}

		public void setTrustStoreAlgorithm(String trustStoreAlgorithm) {
			this.trustStoreAlgorithm = trustStoreAlgorithm;
		}

		public @Nullable String getAlgorithm() {
			return this.algorithm;
		}

		public void setAlgorithm(@Nullable String sslAlgorithm) {
			this.algorithm = sslAlgorithm;
		}

		public boolean isValidateServerCertificate() {
			return this.validateServerCertificate;
		}

		public void setValidateServerCertificate(boolean validateServerCertificate) {
			this.validateServerCertificate = validateServerCertificate;
		}

		public boolean isVerifyHostname() {
			return this.verifyHostname;
		}

		public void setVerifyHostname(boolean verifyHostname) {
			this.verifyHostname = verifyHostname;
		}

	}

	public static class Cache {

		private final Channel channel = new Channel();

		private final Connection connection = new Connection();

		public Channel getChannel() {
			return this.channel;
		}

		public Connection getConnection() {
			return this.connection;
		}

		public static class Channel {

			/**
			 * Number of channels to retain in the cache. When "check-timeout" > 0, max
			 * channels per connection.
			 */
			private @Nullable Integer size;

			/**
			 * Duration to wait to obtain a channel if the cache size has been reached. If
			 * 0, always create a new channel.
			 */
			private @Nullable Duration checkoutTimeout;

			public @Nullable Integer getSize() {
				return this.size;
			}

			public void setSize(@Nullable Integer size) {
				this.size = size;
			}

			public @Nullable Duration getCheckoutTimeout() {
				return this.checkoutTimeout;
			}

			public void setCheckoutTimeout(@Nullable Duration checkoutTimeout) {
				this.checkoutTimeout = checkoutTimeout;
			}

		}

		public static class Connection {

			/**
			 * Connection factory cache mode.
			 */
			private CacheMode mode = CacheMode.CHANNEL;

			/**
			 * Number of connections to cache. Only applies when mode is CONNECTION.
			 */
			private @Nullable Integer size;

			public CacheMode getMode() {
				return this.mode;
			}

			public void setMode(CacheMode mode) {
				this.mode = mode;
			}

			public @Nullable Integer getSize() {
				return this.size;
			}

			public void setSize(@Nullable Integer size) {
				this.size = size;
			}

		}

	}

	public enum ContainerType {

		/**
		 * Container where the RabbitMQ consumer dispatches messages to an invoker thread.
		 */
		SIMPLE,

		/**
		 * Container where the listener is invoked directly on the RabbitMQ consumer
		 * thread.
		 */
		DIRECT,

		/**
		 * Container that uses the RabbitMQ Stream Client.
		 */
		STREAM

	}

	public static class Listener {

		/**
		 * Listener container type.
		 */
		private ContainerType type = ContainerType.SIMPLE;

		private final SimpleContainer simple = new SimpleContainer();

		private final DirectContainer direct = new DirectContainer();

		private final StreamContainer stream = new StreamContainer();

		public ContainerType getType() {
			return this.type;
		}

		public void setType(ContainerType containerType) {
			this.type = containerType;
		}

		public SimpleContainer getSimple() {
			return this.simple;
		}

		public DirectContainer getDirect() {
			return this.direct;
		}

		public StreamContainer getStream() {
			return this.stream;
		}

	}

	public abstract static class BaseContainer {

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

	}

	public abstract static class AmqpContainer extends BaseContainer {

		/**
		 * Whether to start the container automatically on startup.
		 */
		private boolean autoStartup = true;

		/**
		 * Acknowledge mode of container.
		 */
		private @Nullable AcknowledgeMode acknowledgeMode;

		/**
		 * Maximum number of unacknowledged messages that can be outstanding at each
		 * consumer.
		 */
		private @Nullable Integer prefetch;

		/**
		 * Whether rejected deliveries are re-queued by default.
		 */
		private @Nullable Boolean defaultRequeueRejected;

		/**
		 * How often idle container events should be published.
		 */
		private @Nullable Duration idleEventInterval;

		/**
		 * Whether the container should present batched messages as discrete messages or
		 * call the listener with the batch.
		 */
		private boolean deBatchingEnabled = true;

		/**
		 * Whether the container (when stopped) should stop immediately after processing
		 * the current message or stop after processing all pre-fetched messages.
		 */
		private boolean forceStop;

		/**
		 * Optional properties for a retry interceptor.
		 */
		private final ListenerRetry retry = new ListenerRetry();

		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		public @Nullable AcknowledgeMode getAcknowledgeMode() {
			return this.acknowledgeMode;
		}

		public void setAcknowledgeMode(@Nullable AcknowledgeMode acknowledgeMode) {
			this.acknowledgeMode = acknowledgeMode;
		}

		public @Nullable Integer getPrefetch() {
			return this.prefetch;
		}

		public void setPrefetch(@Nullable Integer prefetch) {
			this.prefetch = prefetch;
		}

		public @Nullable Boolean getDefaultRequeueRejected() {
			return this.defaultRequeueRejected;
		}

		public void setDefaultRequeueRejected(@Nullable Boolean defaultRequeueRejected) {
			this.defaultRequeueRejected = defaultRequeueRejected;
		}

		public @Nullable Duration getIdleEventInterval() {
			return this.idleEventInterval;
		}

		public void setIdleEventInterval(@Nullable Duration idleEventInterval) {
			this.idleEventInterval = idleEventInterval;
		}

		public abstract boolean isMissingQueuesFatal();

		public boolean isDeBatchingEnabled() {
			return this.deBatchingEnabled;
		}

		public void setDeBatchingEnabled(boolean deBatchingEnabled) {
			this.deBatchingEnabled = deBatchingEnabled;
		}

		public boolean isForceStop() {
			return this.forceStop;
		}

		public void setForceStop(boolean forceStop) {
			this.forceStop = forceStop;
		}

		public ListenerRetry getRetry() {
			return this.retry;
		}

	}

	/**
	 * Configuration properties for {@code SimpleMessageListenerContainer}.
	 */
	public static class SimpleContainer extends AmqpContainer {

		/**
		 * Minimum number of listener invoker threads.
		 */
		private @Nullable Integer concurrency;

		/**
		 * Maximum number of listener invoker threads.
		 */
		private @Nullable Integer maxConcurrency;

		/**
		 * Batch size, expressed as the number of physical messages, to be used by the
		 * container.
		 */
		private @Nullable Integer batchSize;

		/**
		 * Whether to fail if the queues declared by the container are not available on
		 * the broker and/or whether to stop the container if one or more queues are
		 * deleted at runtime.
		 */
		private boolean missingQueuesFatal = true;

		/**
		 * Whether the container creates a batch of messages based on the
		 * 'receive-timeout' and 'batch-size'. Coerces 'de-batching-enabled' to true to
		 * include the contents of a producer created batch in the batch as discrete
		 * records.
		 */
		private boolean consumerBatchEnabled;

		public @Nullable Integer getConcurrency() {
			return this.concurrency;
		}

		public void setConcurrency(@Nullable Integer concurrency) {
			this.concurrency = concurrency;
		}

		public @Nullable Integer getMaxConcurrency() {
			return this.maxConcurrency;
		}

		public void setMaxConcurrency(@Nullable Integer maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
		}

		public @Nullable Integer getBatchSize() {
			return this.batchSize;
		}

		public void setBatchSize(@Nullable Integer batchSize) {
			this.batchSize = batchSize;
		}

		@Override
		public boolean isMissingQueuesFatal() {
			return this.missingQueuesFatal;
		}

		public void setMissingQueuesFatal(boolean missingQueuesFatal) {
			this.missingQueuesFatal = missingQueuesFatal;
		}

		public boolean isConsumerBatchEnabled() {
			return this.consumerBatchEnabled;
		}

		public void setConsumerBatchEnabled(boolean consumerBatchEnabled) {
			this.consumerBatchEnabled = consumerBatchEnabled;
		}

	}

	/**
	 * Configuration properties for {@code DirectMessageListenerContainer}.
	 */
	public static class DirectContainer extends AmqpContainer {

		/**
		 * Number of consumers per queue.
		 */
		private @Nullable Integer consumersPerQueue;

		/**
		 * Whether to fail if the queues declared by the container are not available on
		 * the broker.
		 */
		private boolean missingQueuesFatal = false;

		public @Nullable Integer getConsumersPerQueue() {
			return this.consumersPerQueue;
		}

		public void setConsumersPerQueue(@Nullable Integer consumersPerQueue) {
			this.consumersPerQueue = consumersPerQueue;
		}

		@Override
		public boolean isMissingQueuesFatal() {
			return this.missingQueuesFatal;
		}

		public void setMissingQueuesFatal(boolean missingQueuesFatal) {
			this.missingQueuesFatal = missingQueuesFatal;
		}

	}

	public static class StreamContainer extends BaseContainer {

		/**
		 * Whether the container will support listeners that consume native stream
		 * messages instead of Spring AMQP messages.
		 */
		private boolean nativeListener;

		public boolean isNativeListener() {
			return this.nativeListener;
		}

		public void setNativeListener(boolean nativeListener) {
			this.nativeListener = nativeListener;
		}

	}

	public static class Template {

		private final Retry retry = new Retry();

		/**
		 * Whether to enable mandatory messages.
		 */
		private @Nullable Boolean mandatory;

		/**
		 * Timeout for receive() operations.
		 */
		private @Nullable Duration receiveTimeout;

		/**
		 * Timeout for sendAndReceive() operations.
		 */
		private @Nullable Duration replyTimeout;

		/**
		 * Name of the default exchange to use for send operations.
		 */
		private String exchange = "";

		/**
		 * Value of a default routing key to use for send operations.
		 */
		private String routingKey = "";

		/**
		 * Name of the default queue to receive messages from when none is specified
		 * explicitly.
		 */
		private @Nullable String defaultReceiveQueue;

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		/**
		 * Simple patterns for allowable packages/classes for deserialization.
		 */
		private @Nullable List<String> allowedListPatterns;

		public Retry getRetry() {
			return this.retry;
		}

		public @Nullable Boolean getMandatory() {
			return this.mandatory;
		}

		public void setMandatory(@Nullable Boolean mandatory) {
			this.mandatory = mandatory;
		}

		public @Nullable Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(@Nullable Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public @Nullable Duration getReplyTimeout() {
			return this.replyTimeout;
		}

		public void setReplyTimeout(@Nullable Duration replyTimeout) {
			this.replyTimeout = replyTimeout;
		}

		public String getExchange() {
			return this.exchange;
		}

		public void setExchange(String exchange) {
			this.exchange = exchange;
		}

		public String getRoutingKey() {
			return this.routingKey;
		}

		public void setRoutingKey(String routingKey) {
			this.routingKey = routingKey;
		}

		public @Nullable String getDefaultReceiveQueue() {
			return this.defaultReceiveQueue;
		}

		public void setDefaultReceiveQueue(@Nullable String defaultReceiveQueue) {
			this.defaultReceiveQueue = defaultReceiveQueue;
		}

		public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

		public @Nullable List<String> getAllowedListPatterns() {
			return this.allowedListPatterns;
		}

		public void setAllowedListPatterns(@Nullable List<String> allowedListPatterns) {
			this.allowedListPatterns = allowedListPatterns;
		}

	}

	public static class Retry {

		/**
		 * Whether publishing retries are enabled.
		 */
		private boolean enabled;

		/**
		 * Maximum number of attempts to deliver a message.
		 */
		private long maxAttempts = 3;

		/**
		 * Duration between the first and second attempt to deliver a message.
		 */
		private Duration initialInterval = Duration.ofMillis(1000);

		/**
		 * Multiplier to apply to the previous retry interval.
		 */
		private double multiplier = 1.0;

		/**
		 * Maximum duration between attempts.
		 */
		private Duration maxInterval = Duration.ofMillis(10000);

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public long getMaxAttempts() {
			return this.maxAttempts;
		}

		public void setMaxAttempts(long maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public Duration getInitialInterval() {
			return this.initialInterval;
		}

		public void setInitialInterval(Duration initialInterval) {
			this.initialInterval = initialInterval;
		}

		public double getMultiplier() {
			return this.multiplier;
		}

		public void setMultiplier(double multiplier) {
			this.multiplier = multiplier;
		}

		public Duration getMaxInterval() {
			return this.maxInterval;
		}

		public void setMaxInterval(Duration maxInterval) {
			this.maxInterval = maxInterval;
		}

		RetryPolicySettings initializeRetryPolicySettings() {
			PropertyMapper map = PropertyMapper.get();
			RetryPolicySettings settings = new RetryPolicySettings();
			map.from(this::getMaxAttempts).to(settings::setMaxAttempts);
			map.from(this::getInitialInterval).to(settings::setDelay);
			map.from(this::getMultiplier).to(settings::setMultiplier);
			map.from(this::getMaxInterval).to(settings::setMaxDelay);
			return settings;
		}

	}

	public static class ListenerRetry extends Retry {

		/**
		 * Whether retries are stateless or stateful.
		 */
		private boolean stateless = true;

		public boolean isStateless() {
			return this.stateless;
		}

		public void setStateless(boolean stateless) {
			this.stateless = stateless;
		}

	}

	private static final class Address {

		private static final String PREFIX_AMQP = "amqp://";

		private static final String PREFIX_AMQP_SECURE = "amqps://";

		private String host;

		private int port;

		private @Nullable String username;

		private @Nullable String password;

		private @Nullable String virtualHost;

		private @Nullable Boolean secureConnection;

		private Address(String input, boolean sslEnabled) {
			input = input.trim();
			input = trimPrefix(input);
			input = parseUsernameAndPassword(input);
			input = parseVirtualHost(input);
			parseHostAndPort(input, sslEnabled);
		}

		private String trimPrefix(String input) {
			if (input.startsWith(PREFIX_AMQP_SECURE)) {
				this.secureConnection = true;
				return input.substring(PREFIX_AMQP_SECURE.length());
			}
			if (input.startsWith(PREFIX_AMQP)) {
				this.secureConnection = false;
				return input.substring(PREFIX_AMQP.length());
			}
			return input;
		}

		private String parseUsernameAndPassword(String input) {
			String[] splitInput = StringUtils.split(input, "@");
			if (splitInput == null) {
				return input;
			}
			String credentials = splitInput[0];
			String[] splitCredentials = StringUtils.split(credentials, ":");
			if (splitCredentials == null) {
				this.username = credentials;
			}
			else {
				this.username = splitCredentials[0];
				this.password = splitCredentials[1];
			}
			return splitInput[1];
		}

		private String parseVirtualHost(String input) {
			int hostIndex = input.indexOf('/');
			if (hostIndex >= 0) {
				this.virtualHost = input.substring(hostIndex + 1);
				if (this.virtualHost.isEmpty()) {
					this.virtualHost = "/";
				}
				input = input.substring(0, hostIndex);
			}
			return input;
		}

		private void parseHostAndPort(String input, boolean sslEnabled) {
			int bracketIndex = input.lastIndexOf(']');
			int colonIndex = input.lastIndexOf(':');
			if (colonIndex == -1 || colonIndex < bracketIndex) {
				this.host = input;
				this.port = (determineSslEnabled(sslEnabled)) ? DEFAULT_PORT_SECURE : DEFAULT_PORT;
			}
			else {
				this.host = input.substring(0, colonIndex);
				this.port = Integer.parseInt(input.substring(colonIndex + 1));
			}
		}

		private boolean determineSslEnabled(boolean sslEnabled) {
			return (this.secureConnection != null) ? this.secureConnection : sslEnabled;
		}

	}

	public static final class Stream {

		/**
		 * Host of a RabbitMQ instance with the Stream plugin enabled.
		 */
		private String host = "localhost";

		/**
		 * Stream port of a RabbitMQ instance with the Stream plugin enabled.
		 */
		private int port = DEFAULT_STREAM_PORT;

		/**
		 * Virtual host of a RabbitMQ instance with the Stream plugin enabled. When not
		 * set, spring.rabbitmq.virtual-host is used.
		 */
		private @Nullable String virtualHost;

		/**
		 * Login user to authenticate to the broker. When not set,
		 * spring.rabbitmq.username is used.
		 */
		private @Nullable String username;

		/**
		 * Login password to authenticate to the broker. When not set
		 * spring.rabbitmq.password is used.
		 */
		private @Nullable String password;

		/**
		 * Name of the stream.
		 */
		private @Nullable String name;

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public @Nullable String getVirtualHost() {
			return this.virtualHost;
		}

		public void setVirtualHost(@Nullable String virtualHost) {
			this.virtualHost = virtualHost;
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

		public @Nullable String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

	}

}
