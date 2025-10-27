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

package org.springframework.boot.elasticsearch.autoconfigure;

import java.io.InputStream;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ElasticsearchRestClientAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Evgeniy Cheban
 * @author Filip Hrisafov
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchRestClientAutoConfigurationIntegrationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = TestImage.container(ElasticsearchContainer.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class));

	@Test
	void restClientCanQueryElasticsearchNode() {
		this.contextRunner
			.withPropertyValues("spring.elasticsearch.uris=" + elasticsearch.getHttpHostAddress(),
					"spring.elasticsearch.connection-timeout=120s", "spring.elasticsearch.socket-timeout=120s")
			.run((context) -> {
				Rest5Client client = context.getBean(Rest5Client.class);
				Request index = new Request("PUT", "/test/_doc/2");
				index.setJsonEntity("{" + "  \"a\": \"alpha\"," + "  \"b\": \"bravo\"" + "}");
				client.performRequest(index);
				Request getRequest = new Request("GET", "/test/_doc/2");
				Response response = client.performRequest(getRequest);
				try (InputStream input = response.getEntity().getContent()) {
					JsonNode result = new ObjectMapper().readTree(input);
					assertThat(result.path("found").asBoolean()).isTrue();
				}
			});
	}

}
