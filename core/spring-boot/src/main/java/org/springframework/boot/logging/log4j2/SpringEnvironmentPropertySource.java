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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.util.PropertySource;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;

/**
 * Returns properties from Spring.
 *
 * @author Ralph Goers
 */
class SpringEnvironmentPropertySource implements PropertySource {

	/**
	 * System properties take precedence followed by properties in Log4j properties files.
	 */
	private static final int PRIORITY = -100;

	private volatile @Nullable Environment environment;

	@Override
	public int getPriority() {
		return PRIORITY;
	}

	@Override
	public @Nullable String getProperty(String key) {
		Environment environment = this.environment;
		return (environment != null) ? environment.getProperty(key) : null;
	}

	@Override
	public boolean containsProperty(String key) {
		Environment environment = this.environment;
		return environment != null && environment.containsProperty(key);
	}

	void setEnvironment(@Nullable Environment environment) {
		this.environment = environment;
	}

}
