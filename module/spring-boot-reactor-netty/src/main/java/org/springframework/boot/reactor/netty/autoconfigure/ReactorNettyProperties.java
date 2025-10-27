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

package org.springframework.boot.reactor.netty.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Reactor Netty.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("spring.reactor.netty")
public class ReactorNettyProperties {

	/**
	 * Amount of time to wait before shutting down resources.
	 */
	private @Nullable Duration shutdownQuietPeriod;

	public @Nullable Duration getShutdownQuietPeriod() {
		return this.shutdownQuietPeriod;
	}

	public void setShutdownQuietPeriod(@Nullable Duration shutdownQuietPeriod) {
		this.shutdownQuietPeriod = shutdownQuietPeriod;
	}

}
