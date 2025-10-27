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

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.AbstractPropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

/**
 * Alternative {@link PropertySourcesPropertyResolver} implementation that recognizes
 * {@link ConfigurationPropertySourcesPropertySource} and saves duplicate calls to the
 * underlying sources if the name is a value {@link ConfigurationPropertyName}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesPropertyResolver extends AbstractPropertyResolver {

	private final MutablePropertySources propertySources;

	private final DefaultResolver defaultResolver;

	ConfigurationPropertySourcesPropertyResolver(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
		this.defaultResolver = new DefaultResolver(propertySources);
	}

	@Override
	public boolean containsProperty(String key) {
		ConfigurationPropertySourcesPropertySource attached = getAttached();
		if (attached != null) {
			ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
			if (name != null) {
				try {
					return attached.findConfigurationProperty(name) != null;
				}
				catch (Exception ex) {
					// Ignore
				}
			}
		}
		return this.defaultResolver.containsProperty(key);
	}

	@Override
	public @Nullable String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	@Override
	public <T> @Nullable T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	@Override
	protected @Nullable String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	private <T> @Nullable T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		Object value = findPropertyValue(key);
		if (value == null) {
			return null;
		}
		if (resolveNestedPlaceholders && value instanceof String string) {
			value = resolveNestedPlaceholders(string);
		}
		try {
			return convertValueIfNecessary(value, targetValueType);
		}
		catch (ConversionFailedException ex) {
			Exception wrappedCause = new InvalidConfigurationPropertyValueException(key, value,
					"Failed to convert to type " + ex.getTargetType(), ex.getCause());
			throw new ConversionFailedException(ex.getSourceType(), ex.getTargetType(), ex.getValue(), wrappedCause);
		}
	}

	private @Nullable Object findPropertyValue(String key) {
		ConfigurationPropertySourcesPropertySource attached = getAttached();
		if (attached != null) {
			ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
			if (name != null) {
				try {
					ConfigurationProperty configurationProperty = attached.findConfigurationProperty(name);
					return (configurationProperty != null) ? configurationProperty.getValue() : null;
				}
				catch (Exception ex) {
					// Ignore
				}
			}
		}
		return this.defaultResolver.getProperty(key, Object.class, false);
	}

	private @Nullable ConfigurationPropertySourcesPropertySource getAttached() {
		ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) ConfigurationPropertySources
			.getAttached(this.propertySources);
		Iterable<ConfigurationPropertySource> attachedSource = (attached != null) ? attached.getSource() : null;
		if ((attachedSource instanceof SpringConfigurationPropertySources springSource)
				&& springSource.isUsingSources(this.propertySources)) {
			return attached;
		}
		return null;
	}

	/**
	 * Default {@link PropertySourcesPropertyResolver} used if
	 * {@link ConfigurationPropertySources} is not attached.
	 */
	static class DefaultResolver extends PropertySourcesPropertyResolver {

		DefaultResolver(PropertySources propertySources) {
			super(propertySources);
		}

		@Override
		public <T> @Nullable T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
			return super.getProperty(key, targetValueType, resolveNestedPlaceholders);
		}

	}

}
