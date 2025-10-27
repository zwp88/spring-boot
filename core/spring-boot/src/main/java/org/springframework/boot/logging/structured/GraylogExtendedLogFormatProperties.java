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

package org.springframework.boot.logging.structured;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.json.JsonWriter;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Service details for Graylog Extended Log Format structured logging.
 *
 * @param host the application name
 * @param service the version of the application
 * @author Samuel Lissner
 * @author Phillip Webb
 * @since 3.4.0
 */
public record GraylogExtendedLogFormatProperties(@Nullable String host, Service service) {

	static final GraylogExtendedLogFormatProperties NONE = new GraylogExtendedLogFormatProperties(null, null);

	public GraylogExtendedLogFormatProperties(@Nullable String host, @Nullable Service service) {
		this.host = host;
		this.service = (service != null) ? service : Service.NONE;
	}

	GraylogExtendedLogFormatProperties withDefaults(Environment environment) {
		String name = withFallbackProperty(environment, this.host, "spring.application.name");
		Service service = this.service.withDefaults(environment);
		return new GraylogExtendedLogFormatProperties(name, service);
	}

	static @Nullable String withFallbackProperty(Environment environment, @Nullable String value, String property) {
		return (!StringUtils.hasLength(value)) ? environment.getProperty(property) : value;
	}

	/**
	 * Add {@link JsonWriter} members for the service.
	 * @param members the members to add to
	 */
	@SuppressWarnings("NullAway") // Doesn't detect lambda with correct nullability
	public void jsonMembers(JsonWriter.Members<?> members) {
		members.add("host", this::host).whenHasLength();
		this.service.jsonMembers(members);
	}

	/**
	 * Return a new {@link GraylogExtendedLogFormatProperties} from bound from properties
	 * in the given {@link Environment}.
	 * @param environment the source environment
	 * @return a new {@link GraylogExtendedLogFormatProperties} instance
	 */
	public static GraylogExtendedLogFormatProperties get(Environment environment) {
		return Binder.get(environment)
			.bind("logging.structured.gelf", GraylogExtendedLogFormatProperties.class)
			.orElse(NONE)
			.withDefaults(environment);
	}

	/**
	 * Service details.
	 *
	 * @param version the version of the application
	 */
	public record Service(@Nullable String version) {

		static final Service NONE = new Service(null);

		Service withDefaults(Environment environment) {
			String version = withFallbackProperty(environment, this.version, "spring.application.version");
			return new Service(version);
		}

		@SuppressWarnings("NullAway") // Doesn't detect lambda with correct nullability
		void jsonMembers(JsonWriter.Members<?> members) {
			members.add("_service_version", this::version).whenHasLength();
		}

	}

	static class GraylogExtendedLogFormatPropertiesRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			BindableRuntimeHintsRegistrar.forTypes(GraylogExtendedLogFormatProperties.class)
				.registerHints(hints, classLoader);
		}

	}

}
