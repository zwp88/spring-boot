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

package org.springframework.boot.cache.autoconfigure.metrics;

import java.util.Collection;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.cache.metrics.CacheMeterBinderProvider;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configure a {@link CacheMetricsRegistrar} and register all available {@link Cache
 * caches}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({ CacheMeterBinderProvider.class, MeterRegistry.class })
class CacheMetricsRegistrarConfiguration {

	private static final String CACHE_MANAGER_SUFFIX = "cacheManager";

	private final MeterRegistry registry;

	private final CacheMetricsRegistrar cacheMetricsRegistrar;

	private final Map<String, CacheManager> cacheManagers;

	CacheMetricsRegistrarConfiguration(MeterRegistry registry, Collection<CacheMeterBinderProvider<?>> binderProviders,
			ConfigurableListableBeanFactory beanFactory) {
		this.registry = registry;
		this.cacheManagers = SimpleAutowireCandidateResolver.resolveAutowireCandidates(beanFactory, CacheManager.class);
		this.cacheMetricsRegistrar = new CacheMetricsRegistrar(this.registry, binderProviders);
		bindCachesToRegistry();
	}

	@Bean
	CacheMetricsRegistrar cacheMetricsRegistrar() {
		return this.cacheMetricsRegistrar;
	}

	private void bindCachesToRegistry() {
		this.cacheManagers.forEach(this::bindCacheManagerToRegistry);
	}

	private void bindCacheManagerToRegistry(String beanName, CacheManager cacheManager) {
		cacheManager.getCacheNames().forEach((cacheName) -> {
			Cache cache = cacheManager.getCache(cacheName);
			Assert.state(cache != null, () -> "'cache' must not be null. 'cacheName' is '%s'".formatted(cacheName));
			bindCacheToRegistry(beanName, cache);
		});
	}

	private void bindCacheToRegistry(String beanName, Cache cache) {
		Tag cacheManagerTag = Tag.of("cache.manager", getCacheManagerName(beanName));
		this.cacheMetricsRegistrar.bindCacheToRegistry(cache, cacheManagerTag);
	}

	/**
	 * Get the name of a {@link CacheManager} based on its {@code beanName}.
	 * @param beanName the name of the {@link CacheManager} bean
	 * @return a name for the given cache manager
	 */
	private String getCacheManagerName(String beanName) {
		if (beanName.length() > CACHE_MANAGER_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, CACHE_MANAGER_SUFFIX)) {
			return beanName.substring(0, beanName.length() - CACHE_MANAGER_SUFFIX.length());
		}
		return beanName;
	}

}
