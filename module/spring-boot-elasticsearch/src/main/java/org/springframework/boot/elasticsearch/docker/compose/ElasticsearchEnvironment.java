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

package org.springframework.boot.elasticsearch.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Elasticsearch environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ElasticsearchEnvironment {

	private final @Nullable String password;

	ElasticsearchEnvironment(Map<String, @Nullable String> env) {
		Assert.state(!env.containsKey("ELASTIC_PASSWORD_FILE"), "ELASTIC_PASSWORD_FILE is not supported");
		this.password = env.get("ELASTIC_PASSWORD");
	}

	@Nullable String getPassword() {
		return this.password;
	}

}
