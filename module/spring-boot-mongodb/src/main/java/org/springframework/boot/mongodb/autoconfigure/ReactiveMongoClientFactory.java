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

package org.springframework.boot.mongodb.autoconfigure;

import java.util.List;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.jspecify.annotations.Nullable;

/**
 * A factory for a reactive {@link MongoClient}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 4.0.0
 */
public class ReactiveMongoClientFactory extends MongoClientFactorySupport<MongoClient> {

	/**
	 * Construct a factory for creating a {@link MongoClient}.
	 * @param builderCustomizers a list of configuration settings customizers
	 */
	public ReactiveMongoClientFactory(@Nullable List<MongoClientSettingsBuilderCustomizer> builderCustomizers) {
		super(builderCustomizers, MongoClients::create);
	}

}
