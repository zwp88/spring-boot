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

import org.springframework.boot.health.contributor.Status;

/**
 * Strategy used to map a {@link Status health status} to an HTTP status code.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface HttpCodeStatusMapper {

	/**
	 * An {@link HttpCodeStatusMapper} instance using default mappings.
	 */
	HttpCodeStatusMapper DEFAULT = new SimpleHttpCodeStatusMapper();

	/**
	 * Return the HTTP status code that corresponds to the given {@link Status health
	 * status}.
	 * @param status the health status to map
	 * @return the corresponding HTTP status code
	 */
	int getStatusCode(Status status);

}
