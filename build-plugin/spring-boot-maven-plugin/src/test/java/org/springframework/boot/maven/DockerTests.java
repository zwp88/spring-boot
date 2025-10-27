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

package org.springframework.boot.maven;

import java.util.Base64;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.build.BuilderDockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConnectionConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryAuthentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Docker}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
class DockerTests {

	private final Log log = new SystemStreamLog();

	@Test
	void asDockerConfigurationWithDefaults() {
		Docker docker = new Docker();
		BuilderDockerConfiguration dockerConfiguration = createDockerConfiguration(docker);
		assertThat(dockerConfiguration.connection()).isNull();
		DockerRegistryAuthentication builderRegistryAuthentication = dockerConfiguration
			.builderRegistryAuthentication();
		assertThat(builderRegistryAuthentication).isNotNull();
		assertThat(builderRegistryAuthentication.getAuthHeader()).isNull();
		DockerRegistryAuthentication publishRegistryAuthentication = dockerConfiguration
			.publishRegistryAuthentication();
		assertThat(publishRegistryAuthentication).isNotNull();
		String authHeader = publishRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithHostConfiguration() {
		Docker docker = new Docker();
		docker.setHost("docker.example.com");
		docker.setTlsVerify(true);
		docker.setCertPath("/tmp/ca-cert");
		BuilderDockerConfiguration dockerConfiguration = createDockerConfiguration(docker);
		DockerConnectionConfiguration.Host host = (DockerConnectionConfiguration.Host) dockerConfiguration.connection();
		assertThat(host).isNotNull();
		assertThat(host.address()).isEqualTo("docker.example.com");
		assertThat(host.secure()).isTrue();
		assertThat(host.certificatePath()).isEqualTo("/tmp/ca-cert");
		assertThat(dockerConfiguration.bindHostToBuilder()).isFalse();
		DockerRegistryAuthentication builderRegistryAuthentication = createDockerConfiguration(docker)
			.builderRegistryAuthentication();
		assertThat(builderRegistryAuthentication).isNotNull();
		assertThat(builderRegistryAuthentication.getAuthHeader()).isNull();
		DockerRegistryAuthentication publishRegistryAuthentication = dockerConfiguration
			.publishRegistryAuthentication();
		assertThat(publishRegistryAuthentication).isNotNull();
		String authHeader = publishRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithContextConfiguration() {
		Docker docker = new Docker();
		docker.setContext("test-context");
		BuilderDockerConfiguration dockerConfiguration = createDockerConfiguration(docker);
		DockerConnectionConfiguration.Context context = (DockerConnectionConfiguration.Context) dockerConfiguration
			.connection();
		assertThat(context).isNotNull();
		assertThat(context.context()).isEqualTo("test-context");
		assertThat(dockerConfiguration.bindHostToBuilder()).isFalse();
		DockerRegistryAuthentication builderRegistryAuthentication = createDockerConfiguration(docker)
			.builderRegistryAuthentication();
		assertThat(builderRegistryAuthentication).isNotNull();
		assertThat(builderRegistryAuthentication.getAuthHeader()).isNull();
		DockerRegistryAuthentication publishRegistryAuthentication = dockerConfiguration
			.publishRegistryAuthentication();
		assertThat(publishRegistryAuthentication).isNotNull();
		String authHeader = publishRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithHostAndContextFails() {
		Docker docker = new Docker();
		docker.setContext("test-context");
		docker.setHost("docker.example.com");
		assertThatIllegalArgumentException().isThrownBy(() -> createDockerConfiguration(docker))
			.withMessageContaining("Invalid Docker configuration");
	}

	@Test
	void asDockerConfigurationWithBindHostToBuilder() {
		Docker docker = new Docker();
		docker.setHost("docker.example.com");
		docker.setTlsVerify(true);
		docker.setCertPath("/tmp/ca-cert");
		docker.setBindHostToBuilder(true);
		BuilderDockerConfiguration dockerConfiguration = createDockerConfiguration(docker);
		DockerConnectionConfiguration.Host host = (DockerConnectionConfiguration.Host) dockerConfiguration.connection();
		assertThat(host).isNotNull();
		assertThat(host.address()).isEqualTo("docker.example.com");
		assertThat(host.secure()).isTrue();
		assertThat(host.certificatePath()).isEqualTo("/tmp/ca-cert");
		assertThat(dockerConfiguration.bindHostToBuilder()).isTrue();
		DockerRegistryAuthentication builderRegistryAuthentication = createDockerConfiguration(docker)
			.builderRegistryAuthentication();
		assertThat(builderRegistryAuthentication).isNotNull();
		assertThat(builderRegistryAuthentication.getAuthHeader()).isNull();
		DockerRegistryAuthentication publishRegistryAuthentication = dockerConfiguration
			.publishRegistryAuthentication();
		assertThat(publishRegistryAuthentication).isNotNull();
		String authHeader = publishRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithUserAuth() {
		Docker docker = new Docker();
		docker.setBuilderRegistry(
				new Docker.DockerRegistry("user1", "secret1", "https://docker1.example.com", "docker1@example.com"));
		docker.setPublishRegistry(
				new Docker.DockerRegistry("user2", "secret2", "https://docker2.example.com", "docker2@example.com"));
		BuilderDockerConfiguration dockerConfiguration = createDockerConfiguration(docker);
		DockerRegistryAuthentication builderRegistryAuthentication = dockerConfiguration
			.builderRegistryAuthentication();
		assertThat(builderRegistryAuthentication).isNotNull();
		String authHeader = builderRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"username\" : \"user1\"")
			.contains("\"password\" : \"secret1\"")
			.contains("\"email\" : \"docker1@example.com\"")
			.contains("\"serveraddress\" : \"https://docker1.example.com\"");
		DockerRegistryAuthentication publishRegistryAuthentication = dockerConfiguration
			.publishRegistryAuthentication();
		assertThat(publishRegistryAuthentication).isNotNull();
		authHeader = publishRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"username\" : \"user2\"")
			.contains("\"password\" : \"secret2\"")
			.contains("\"email\" : \"docker2@example.com\"")
			.contains("\"serveraddress\" : \"https://docker2.example.com\"");
	}

	@Test
	void asDockerConfigurationWithIncompleteBuilderUserAuthFails() {
		Docker docker = new Docker();
		docker.setBuilderRegistry(
				new Docker.DockerRegistry("user", null, "https://docker.example.com", "docker@example.com"));
		assertThatIllegalArgumentException().isThrownBy(() -> createDockerConfiguration(docker))
			.withMessageContaining("Invalid Docker builder registry configuration");
	}

	@Test
	void asDockerConfigurationWithIncompletePublishUserAuthFails() {
		Docker docker = new Docker();
		docker.setPublishRegistry(
				new Docker.DockerRegistry("user", null, "https://docker.example.com", "docker@example.com"));
		assertThatIllegalArgumentException().isThrownBy(() -> createDockerConfiguration(docker))
			.withMessageContaining("Invalid Docker publish registry configuration");
	}

	@Test
	void asDockerConfigurationWithIncompletePublishUserAuthDoesNotFailIfPublishIsDisabled() {
		Docker docker = new Docker();
		docker.setPublishRegistry(
				new Docker.DockerRegistry("user", null, "https://docker.example.com", "docker@example.com"));
		BuilderDockerConfiguration dockerConfiguration = docker.asDockerConfiguration(this.log, false);
		assertThat(dockerConfiguration.publishRegistryAuthentication()).isNull();
	}

	@Test
	void asDockerConfigurationWithTokenAuth() {
		Docker docker = new Docker();
		docker.setBuilderRegistry(new Docker.DockerRegistry("token1"));
		docker.setPublishRegistry(new Docker.DockerRegistry("token2"));
		BuilderDockerConfiguration dockerConfiguration = createDockerConfiguration(docker);
		DockerRegistryAuthentication builderRegistryAuthentication = dockerConfiguration
			.builderRegistryAuthentication();
		assertThat(builderRegistryAuthentication).isNotNull();
		String authHeader = builderRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"identitytoken\" : \"token1\"");
		DockerRegistryAuthentication publishRegistryAuthentication = dockerConfiguration
			.publishRegistryAuthentication();
		assertThat(publishRegistryAuthentication).isNotNull();
		authHeader = publishRegistryAuthentication.getAuthHeader();
		assertThat(authHeader).isNotNull();
		assertThat(decoded(authHeader)).contains("\"identitytoken\" : \"token2\"");
	}

	@Test
	void asDockerConfigurationWithUserAndTokenAuthFails() {
		Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
		dockerRegistry.setUsername("user");
		dockerRegistry.setPassword("secret");
		dockerRegistry.setToken("token");
		Docker docker = new Docker();
		docker.setBuilderRegistry(dockerRegistry);
		assertThatIllegalArgumentException().isThrownBy(() -> createDockerConfiguration(docker))
			.withMessageContaining("Invalid Docker builder registry configuration");
	}

	@Test
	void asDockerConfigurationWithUserAndTokenAuthDoesNotFailIfPublishingIsDisabled() {
		Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
		dockerRegistry.setUsername("user");
		dockerRegistry.setPassword("secret");
		dockerRegistry.setToken("token");
		Docker docker = new Docker();
		docker.setPublishRegistry(dockerRegistry);
		BuilderDockerConfiguration dockerConfiguration = docker.asDockerConfiguration(this.log, false);
		assertThat(dockerConfiguration.publishRegistryAuthentication()).isNull();
	}

	private BuilderDockerConfiguration createDockerConfiguration(Docker docker) {
		return docker.asDockerConfiguration(this.log, true);
	}

	String decoded(String value) {
		return new String(Base64.getDecoder().decode(value));
	}

}
