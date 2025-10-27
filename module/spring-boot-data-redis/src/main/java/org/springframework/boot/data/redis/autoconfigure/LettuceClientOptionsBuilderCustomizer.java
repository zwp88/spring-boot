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

package org.springframework.boot.data.redis.autoconfigure;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.Builder;

import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Builder} to fine-tune its auto-configuration before it creates the
 * {@link ClientOptions} of the {@link LettuceClientConfiguration}. To customize the
 * entire configuration, use {@link LettuceClientConfigurationBuilderCustomizer} instead.
 *
 * @author Soohyun Lim
 * @since 4.0.0
 */
@FunctionalInterface
public interface LettuceClientOptionsBuilderCustomizer {

	/**
	 * Customize the {@link Builder}.
	 * @param clientOptionsBuilder the builder to customize
	 */
	void customize(Builder clientOptionsBuilder);

}
