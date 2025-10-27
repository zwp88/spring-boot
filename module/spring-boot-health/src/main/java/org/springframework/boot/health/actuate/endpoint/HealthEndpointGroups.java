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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.util.Assert;

/**
 * A collection of {@link HealthEndpointGroup groups} for use with a health endpoint.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface HealthEndpointGroups {

	/**
	 * Return the primary group used by the endpoint.
	 * @return the primary group (never {@code null})
	 */
	HealthEndpointGroup getPrimary();

	/**
	 * Return the names of any additional groups.
	 * @return the additional group names
	 */
	Set<String> getNames();

	/**
	 * Return the group with the specified name or {@code null} if the name is not known.
	 * @param name the name of the group
	 * @return the {@link HealthEndpointGroup} or {@code null}
	 */
	@Nullable HealthEndpointGroup get(String name);

	/**
	 * Return the group with the specified additional path or {@code null} if no group
	 * with that path is found.
	 * @param path the additional path
	 * @return the matching {@link HealthEndpointGroup} or {@code null}
	 */
	default @Nullable HealthEndpointGroup get(AdditionalHealthEndpointPath path) {
		Assert.notNull(path, "'path' must not be null");
		for (String name : getNames()) {
			HealthEndpointGroup group = get(name);
			Assert.state(group != null, "'group' must not be null");
			if (path.equals(group.getAdditionalPath())) {
				return group;
			}
		}
		return null;
	}

	/**
	 * Return all the groups with an additional path on the specified
	 * {@link WebServerNamespace}.
	 * @param namespace the {@link WebServerNamespace}
	 * @return the matching groups
	 */
	default Set<HealthEndpointGroup> getAllWithAdditionalPath(WebServerNamespace namespace) {
		Assert.notNull(namespace, "'namespace' must not be null");
		Set<HealthEndpointGroup> filteredGroups = new LinkedHashSet<>();
		getNames().stream()
			.map(this::get)
			.filter((group) -> group != null && group.getAdditionalPath() != null
					&& group.getAdditionalPath().hasNamespace(namespace))
			.forEach(filteredGroups::add);
		return filteredGroups;
	}

	/**
	 * Factory method to create a {@link HealthEndpointGroups} instance.
	 * @param primary the primary group
	 * @param additional the additional groups
	 * @return a new {@link HealthEndpointGroups} instance
	 */
	static HealthEndpointGroups of(HealthEndpointGroup primary, Map<String, HealthEndpointGroup> additional) {
		Assert.notNull(primary, "'primary' must not be null");
		Assert.notNull(additional, "'additional' must not be null");
		return new HealthEndpointGroups() {

			@Override
			public HealthEndpointGroup getPrimary() {
				return primary;
			}

			@Override
			public Set<String> getNames() {
				return additional.keySet();
			}

			@Override
			public @Nullable HealthEndpointGroup get(String name) {
				return additional.get(name);
			}

		};
	}

}
