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

package org.springframework.boot.kafka.testcontainers;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.testcontainers.kafka.KafkaContainer;

import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link KafkaConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link KafkaContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
class ApacheKafkaContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<KafkaContainer, KafkaConnectionDetails> {

	@Override
	protected KafkaConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<KafkaContainer> source) {
		return new ApacheKafkaContainerConnectionDetails(source);
	}

	/**
	 * {@link KafkaConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class ApacheKafkaContainerConnectionDetails extends ContainerConnectionDetails<KafkaContainer>
			implements KafkaConnectionDetails {

		private ApacheKafkaContainerConnectionDetails(ContainerConnectionSource<KafkaContainer> source) {
			super(source);
		}

		@Override
		public List<String> getBootstrapServers() {
			return List.of(getContainer().getBootstrapServers());
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

		@Override
		public String getSecurityProtocol() {
			return (getSslBundle() != null) ? "SSL" : "PLAINTEXT";
		}

	}

}
