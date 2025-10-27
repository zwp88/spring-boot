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

package org.springframework.boot.mongodb.testcontainers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DeprecatedMongoDbContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link MongoDbContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
@Deprecated(since = "4.0.0", forRemoval = true)
class DeprecatedMongoDbContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongoDb = TestImage.container(MongoDBContainer.class);

	@Autowired(required = false)
	private MongoConnectionDetails connectionDetails;

	@Test
	void connectionCanBeMadeToContainer() {
		assertThat(this.connectionDetails).isNotNull();
		MongoClient client = MongoClients.create(this.connectionDetails.getConnectionString());
		assertThat(client.listDatabaseNames()).containsExactly("admin", "config", "local");
	}

}
