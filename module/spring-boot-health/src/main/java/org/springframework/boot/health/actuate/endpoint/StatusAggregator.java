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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

/**
 * Strategy used to aggregate {@link Status} instances.
 * <p>
 * This is required in order to combine subsystem states expressed through
 * {@link Health#getStatus()} into one state for the entire system.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface StatusAggregator {

	/**
	 * Return {@link StatusAggregator} instance using default ordering rules.
	 * @return a {@code StatusAggregator} with default ordering rules.
	 */
	static StatusAggregator getDefault() {
		return SimpleStatusAggregator.INSTANCE;
	}

	/**
	 * Return the aggregate status for the given set of statuses.
	 * @param statuses the statuses to aggregate
	 * @return the aggregate status
	 */
	default Status getAggregateStatus(Status... statuses) {
		return getAggregateStatus(new LinkedHashSet<>(Arrays.asList(statuses)));
	}

	/**
	 * Return the aggregate status for the given set of statuses.
	 * @param statuses the statuses to aggregate
	 * @return the aggregate status
	 */
	Status getAggregateStatus(Set<Status> statuses);

}
