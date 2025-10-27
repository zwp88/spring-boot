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

package org.springframework.boot.context.properties.source;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Interface that can be used to control configuration property source caches.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public interface ConfigurationPropertyCaching {

	/**
	 * Enable caching with an unlimited time-to-live.
	 */
	void enable();

	/**
	 * Disable caching.
	 */
	void disable();

	/**
	 * Set amount of time that an item can live in the cache. Calling this method will
	 * also enable the cache.
	 * @param timeToLive the time to live value.
	 */
	void setTimeToLive(Duration timeToLive);

	/**
	 * Clear the cache and force it to be reloaded on next access.
	 */
	void clear();

	/**
	 * Override caching to temporarily enable it. Once caching is no longer needed the
	 * returned {@link CacheOverride} should be closed to restore previous cache settings.
	 * @return a {@link CacheOverride}
	 * @since 3.5.0
	 */
	CacheOverride override();

	/**
	 * Get for all configuration property sources in the environment.
	 * @param environment the spring environment
	 * @return a caching instance that controls all sources in the environment
	 */
	static ConfigurationPropertyCaching get(Environment environment) {
		return get(environment, null);
	}

	/**
	 * Get for a specific configuration property source in the environment.
	 * @param environment the spring environment
	 * @param underlyingSource the
	 * {@link ConfigurationPropertySource#getUnderlyingSource() underlying source} that
	 * must match
	 * @return a caching instance that controls the matching source
	 */
	static ConfigurationPropertyCaching get(Environment environment, @Nullable Object underlyingSource) {
		Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
		return get(sources, underlyingSource);
	}

	/**
	 * Get for all specified configuration property sources.
	 * @param sources the configuration property sources
	 * @return a caching instance that controls the sources
	 */
	static ConfigurationPropertyCaching get(Iterable<ConfigurationPropertySource> sources) {
		return get(sources, null);
	}

	/**
	 * Get for a specific configuration property source in the specified configuration
	 * property sources.
	 * @param sources the configuration property sources
	 * @param underlyingSource the
	 * {@link ConfigurationPropertySource#getUnderlyingSource() underlying source} that
	 * must match
	 * @return a caching instance that controls the matching source
	 */
	static ConfigurationPropertyCaching get(Iterable<ConfigurationPropertySource> sources,
			@Nullable Object underlyingSource) {
		Assert.notNull(sources, "'sources' must not be null");
		if (underlyingSource == null) {
			return new ConfigurationPropertySourcesCaching(sources);
		}
		for (ConfigurationPropertySource source : sources) {
			if (source.getUnderlyingSource() == underlyingSource) {
				ConfigurationPropertyCaching caching = CachingConfigurationPropertySource.find(source);
				if (caching != null) {
					return caching;
				}
			}
		}
		throw new IllegalStateException("Unable to find cache from configuration property sources");
	}

	/**
	 * {@link AutoCloseable} used to control a
	 * {@link ConfigurationPropertyCaching#override() cache override}.
	 *
	 * @since 3.5.0
	 */
	interface CacheOverride extends AutoCloseable {

		@Override
		void close();

	}

}
