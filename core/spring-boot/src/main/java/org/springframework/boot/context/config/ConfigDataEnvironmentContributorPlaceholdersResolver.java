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

package org.springframework.boot.context.config;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * {@link PlaceholdersResolver} backed by one or more
 * {@link ConfigDataEnvironmentContributor} instances.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Moritz Halbritter
 */
class ConfigDataEnvironmentContributorPlaceholdersResolver implements PlaceholdersResolver {

	private final Iterable<ConfigDataEnvironmentContributor> contributors;

	private final @Nullable ConfigDataActivationContext activationContext;

	private final boolean failOnResolveFromInactiveContributor;

	private final PropertyPlaceholderHelper helper;

	private final @Nullable ConfigDataEnvironmentContributor activeContributor;

	private final ConversionService conversionService;

	ConfigDataEnvironmentContributorPlaceholdersResolver(Iterable<ConfigDataEnvironmentContributor> contributors,
			@Nullable ConfigDataActivationContext activationContext,
			@Nullable ConfigDataEnvironmentContributor activeContributor, boolean failOnResolveFromInactiveContributor,
			ConversionService conversionService) {
		this.contributors = contributors;
		this.activationContext = activationContext;
		this.activeContributor = activeContributor;
		this.failOnResolveFromInactiveContributor = failOnResolveFromInactiveContributor;
		this.conversionService = conversionService;
		this.helper = new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
				SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR,
				SystemPropertyUtils.ESCAPE_CHARACTER, true);
	}

	@Override
	public @Nullable Object resolvePlaceholders(@Nullable Object value) {
		if (value instanceof String string) {
			return this.helper.replacePlaceholders(string, this::resolvePlaceholder);
		}
		return value;
	}

	private @Nullable String resolvePlaceholder(String placeholder) {
		Object result = null;
		for (ConfigDataEnvironmentContributor contributor : this.contributors) {
			PropertySource<?> propertySource = contributor.getPropertySource();
			Object value = (propertySource != null) ? propertySource.getProperty(placeholder) : null;
			if (value != null && !isActive(contributor)) {
				if (this.failOnResolveFromInactiveContributor) {
					ConfigDataResource resource = contributor.getResource();
					Assert.state(propertySource != null, "'propertySource' can't be null here");
					Origin origin = OriginLookup.getOrigin(propertySource, placeholder);
					throw new InactiveConfigDataAccessException(propertySource, resource, placeholder, origin);
				}
				value = null;
			}
			result = (result != null) ? result : value;
		}
		return (result != null) ? convertValueIfNecessary(result) : null;
	}

	private boolean isActive(ConfigDataEnvironmentContributor contributor) {
		if (contributor == this.activeContributor) {
			return true;
		}
		if (contributor.getKind() != Kind.UNBOUND_IMPORT) {
			return contributor.isActive(this.activationContext);
		}
		return contributor.withBoundProperties(this.contributors, this.activationContext)
			.isActive(this.activationContext);
	}

	private @Nullable String convertValueIfNecessary(Object value) {
		return (value instanceof String string) ? string : this.conversionService.convert(value, String.class);
	}

}
