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

package org.springframework.boot.cassandra.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Cassandra environment details.
 *
 * @author Scott Frederick
 */
class CassandraEnvironment {

	private final String datacenter;

	CassandraEnvironment(Map<String, @Nullable String> env) {
		this.datacenter = getDatacenter(env);
	}

	private static String getDatacenter(Map<String, @Nullable String> env) {
		String datacenter = env.getOrDefault("CASSANDRA_DC", env.get("CASSANDRA_DATACENTER"));
		return (datacenter != null) ? datacenter : "datacenter1";
	}

	String getDatacenter() {
		return this.datacenter;
	}

}
