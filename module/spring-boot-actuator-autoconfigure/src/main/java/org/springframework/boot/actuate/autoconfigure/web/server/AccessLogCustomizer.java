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

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.lang.Contract;

/**
 * Base class for a {@link WebServerFactoryCustomizer} that customizes the web server's
 * access log.
 *
 * @param <T> the {@link WebServerFactory} type that can be customized
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public abstract class AccessLogCustomizer<T extends WebServerFactory>
		implements WebServerFactoryCustomizer<T>, Ordered {

	private final @Nullable String prefix;

	protected AccessLogCustomizer(@Nullable String prefix) {
		this.prefix = prefix;
	}

	@Contract("!null -> !null")
	protected @Nullable String customizePrefix(@Nullable String existingPrefix) {
		if (this.prefix == null) {
			return existingPrefix;
		}
		if (existingPrefix == null) {
			return this.prefix;
		}
		if (existingPrefix.startsWith(this.prefix)) {
			return existingPrefix;
		}
		return this.prefix + existingPrefix;
	}

	@Override
	public int getOrder() {
		return 1;
	}

}
