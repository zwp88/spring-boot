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

package org.springframework.boot.buildpack.platform.build;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConnectionConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryAuthentication;

/**
 * {@link Builder} configuration options for Docker.
 *
 * @param connection the Docker host configuration
 * @param bindHostToBuilder if the host resolved from the connection should be bound to
 * the builder
 * @param builderRegistryAuthentication the builder {@link DockerRegistryAuthentication}
 * @param publishRegistryAuthentication the publish {@link DockerRegistryAuthentication}
 * @author Phillip Webb
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 3.5.0
 */
public record BuilderDockerConfiguration(@Nullable DockerConnectionConfiguration connection, boolean bindHostToBuilder,
		@Nullable DockerRegistryAuthentication builderRegistryAuthentication,
		@Nullable DockerRegistryAuthentication publishRegistryAuthentication) {

	public BuilderDockerConfiguration() {
		this(null, false, null, null);
	}

	public BuilderDockerConfiguration withContext(String context) {
		return withConnection(new DockerConnectionConfiguration.Context(context));
	}

	public BuilderDockerConfiguration withHost(String address, boolean secure, @Nullable String certificatePath) {
		return withConnection(new DockerConnectionConfiguration.Host(address, secure, certificatePath));
	}

	private BuilderDockerConfiguration withConnection(DockerConnectionConfiguration hostConfiguration) {
		return new BuilderDockerConfiguration(hostConfiguration, this.bindHostToBuilder,
				this.builderRegistryAuthentication, this.publishRegistryAuthentication);
	}

	public BuilderDockerConfiguration withBindHostToBuilder(boolean bindHostToBuilder) {
		return new BuilderDockerConfiguration(this.connection, bindHostToBuilder, this.builderRegistryAuthentication,
				this.publishRegistryAuthentication);
	}

	public BuilderDockerConfiguration withBuilderRegistryAuthentication(
			DockerRegistryAuthentication builderRegistryAuthentication) {
		return new BuilderDockerConfiguration(this.connection, this.bindHostToBuilder, builderRegistryAuthentication,
				this.publishRegistryAuthentication);

	}

	public BuilderDockerConfiguration withPublishRegistryAuthentication(
			DockerRegistryAuthentication publishRegistryAuthentication) {
		return new BuilderDockerConfiguration(this.connection, this.bindHostToBuilder,
				this.builderRegistryAuthentication, publishRegistryAuthentication);
	}

}
