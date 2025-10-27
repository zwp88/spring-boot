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

package org.springframework.boot.health.actuate.endpoint;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.health.contributor.Status;

/**
 * Description of overall system health.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class SystemHealthDescriptor extends CompositeHealthDescriptor {

	private final @Nullable Set<String> groups;

	SystemHealthDescriptor(ApiVersion apiVersion, Status status, @Nullable Map<String, HealthDescriptor> components,
			@Nullable Set<String> groups) {
		super(apiVersion, status, components);
		this.groups = groups;
	}

	@JsonInclude(Include.NON_EMPTY)
	public @Nullable Set<String> getGroups() {
		return this.groups;
	}

}
