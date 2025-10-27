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

package org.springframework.boot.webflux.actuate.web.mappings;

import org.jspecify.annotations.Nullable;

import org.springframework.web.reactive.DispatcherHandler;

/**
 * A description of a mapping known to a {@link DispatcherHandler}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class DispatcherHandlerMappingDescription {

	private final String predicate;

	private final String handler;

	private final @Nullable DispatcherHandlerMappingDetails details;

	DispatcherHandlerMappingDescription(String predicate, String handler,
			@Nullable DispatcherHandlerMappingDetails details) {
		this.predicate = predicate;
		this.handler = handler;
		this.details = details;
	}

	public String getHandler() {
		return this.handler;
	}

	public String getPredicate() {
		return this.predicate;
	}

	public @Nullable DispatcherHandlerMappingDetails getDetails() {
		return this.details;
	}

}
