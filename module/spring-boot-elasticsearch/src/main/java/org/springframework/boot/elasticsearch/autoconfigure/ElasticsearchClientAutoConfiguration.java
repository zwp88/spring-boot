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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientConfigurations.ElasticsearchClientConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientConfigurations.ElasticsearchTransportConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientConfigurations.JsonpMapperConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch's Java client.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(after = { ElasticsearchRestClientAutoConfiguration.class },
		afterName = { "org.springframework.boot.jsonb.autoconfigure.JsonbAutoConfiguration" })
@ConditionalOnBean(Rest5Client.class)
@ConditionalOnClass(ElasticsearchClient.class)
@Import({ JsonpMapperConfiguration.class, ElasticsearchTransportConfiguration.class,
		ElasticsearchClientConfiguration.class })
public final class ElasticsearchClientAutoConfiguration {

}
