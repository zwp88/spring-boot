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

package org.springframework.boot.health.contributor;

import org.jspecify.annotations.Nullable;

/**
 * Directly contributes {@link Health} information for specific component or subsystem.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public non-sealed interface HealthIndicator extends HealthContributor {

	/**
	 * Return an indication of health.
	 * @param includeDetails if details should be included or removed
	 * @return the health
	 */
	default @Nullable Health health(boolean includeDetails) {
		Health health = health();
		if (health == null) {
			return null;
		}
		return includeDetails ? health : health.withoutDetails();
	}

	/**
	 * Return an indication of health.
	 * @return the health
	 */
	@Nullable Health health();

}
