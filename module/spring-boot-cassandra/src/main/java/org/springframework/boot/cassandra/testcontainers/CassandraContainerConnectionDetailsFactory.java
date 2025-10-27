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

package org.springframework.boot.cassandra.testcontainers;

import java.net.InetSocketAddress;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.testcontainers.cassandra.CassandraContainer;

import org.springframework.boot.cassandra.autoconfigure.CassandraConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link CassandraConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link CassandraContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class CassandraContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<CassandraContainer, CassandraConnectionDetails> {

	@Override
	protected CassandraConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<CassandraContainer> source) {
		return new CassandraContainerConnectionDetails(source);
	}

	/**
	 * {@link CassandraConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class CassandraContainerConnectionDetails
			extends ContainerConnectionDetails<CassandraContainer> implements CassandraConnectionDetails {

		private CassandraContainerConnectionDetails(ContainerConnectionSource<CassandraContainer> source) {
			super(source);
		}

		@Override
		public List<Node> getContactPoints() {
			InetSocketAddress contactPoint = getContainer().getContactPoint();
			return List.of(new Node(contactPoint.getHostString(), contactPoint.getPort()));
		}

		@Override
		public String getUsername() {
			return getContainer().getUsername();
		}

		@Override
		public String getPassword() {
			return getContainer().getPassword();
		}

		@Override
		public String getLocalDatacenter() {
			return getContainer().getLocalDatacenter();
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
