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

import java.util.EventListener;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.config.ConfigData.Options;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * {@link EventListener} to listen to {@link Environment} updates triggered by the
 * {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 * @since 2.4.2
 */
public interface ConfigDataEnvironmentUpdateListener extends EventListener {

	/**
	 * A {@link ConfigDataEnvironmentUpdateListener} that does nothing.
	 */
	ConfigDataEnvironmentUpdateListener NONE = new ConfigDataEnvironmentUpdateListener() {
	};

	/**
	 * Called when a new {@link PropertySource} is added to the {@link Environment}.
	 * @param propertySource the {@link PropertySource} that was added
	 * @param location the original {@link ConfigDataLocation} of the source.
	 * @param resource the {@link ConfigDataResource} of the source.
	 */
	default void onPropertySourceAdded(PropertySource<?> propertySource, @Nullable ConfigDataLocation location,
			@Nullable ConfigDataResource resource) {
	}

	/**
	 * Called when {@link Environment} profiles are set.
	 * @param profiles the profiles being set
	 */
	default void onSetProfiles(Profiles profiles) {
	}

	/**
	 * Called when config data options are obtained for a particular property source.
	 * @param configData the config data
	 * @param propertySource the property source
	 * @param options the options as provided by
	 * {@link ConfigData#getOptions(PropertySource)}
	 * @return the actual options that should be used
	 * @since 3.5.1
	 */
	default ConfigData.Options onConfigDataOptions(ConfigData configData, PropertySource<?> propertySource,
			Options options) {
		return options;
	}

}
