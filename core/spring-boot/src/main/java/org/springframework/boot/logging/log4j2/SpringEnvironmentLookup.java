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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerContextAware;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Lookup for Spring properties.
 *
 * @author Ralph Goers
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
@Plugin(name = "spring", category = StrLookup.CATEGORY)
class SpringEnvironmentLookup implements LoggerContextAware, StrLookup {

	private volatile @Nullable Environment environment;

	@Override
	public @Nullable String lookup(LogEvent event, String key) {
		return lookup(key);
	}

	@Override
	public @Nullable String lookup(String key) {
		Environment environment = this.environment;
		Assert.state(environment != null,
				"Unable to obtain Spring Environment from LoggerContext. "
						+ "This can happen if your log4j2 configuration filename does not end with '-spring' "
						+ "(for example using 'log4j2.xml' instead of 'log4j2-spring.xml')");
		return environment.getProperty(key);
	}

	@Override
	public void setLoggerContext(LoggerContext loggerContext) {
		this.environment = Log4J2LoggingSystem.getEnvironment(loggerContext);
	}

}
