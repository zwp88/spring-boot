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

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.registry.HealthContributorNameValidator;
import org.springframework.util.Assert;

/**
 * {@link HealthContributorNameValidator} to ensure names don't clash with groups.
 *
 * @author Phillip Webb
 */
class GroupsHealthContributorNameValidator implements HealthContributorNameValidator {

	private final Set<String> groupNames;

	GroupsHealthContributorNameValidator(@Nullable HealthEndpointGroups groups) {
		this.groupNames = (groups != null) ? groups.getNames() : Collections.emptySet();
	}

	@Override
	public void validate(String name) throws IllegalStateException {
		Assert.state(!this.groupNames.contains(name),
				() -> "HealthContributor with name \"" + name + "\" clashes with group");
	}

}
