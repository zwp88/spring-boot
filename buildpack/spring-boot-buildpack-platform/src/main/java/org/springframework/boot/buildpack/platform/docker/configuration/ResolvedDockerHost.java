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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.jna.Platform;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.DockerContext;
import org.springframework.boot.buildpack.platform.system.Environment;

/**
 * Resolves a {@link DockerHost} from the environment, configuration, or using defaults.
 *
 * @author Scott Frederick
 * @since 2.7.0
 */
public class ResolvedDockerHost extends DockerHost {

	private static final String UNIX_SOCKET_PREFIX = "unix://";

	private static final String DOMAIN_SOCKET_PATH = "/var/run/docker.sock";

	private static final String WINDOWS_NAMED_PIPE_PATH = "//./pipe/docker_engine";

	private static final String DOCKER_HOST = "DOCKER_HOST";

	private static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";

	private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

	private static final String DOCKER_CONTEXT = "DOCKER_CONTEXT";

	ResolvedDockerHost(@Nullable String address) {
		super(address);
	}

	ResolvedDockerHost(@Nullable String address, boolean secure, @Nullable String certificatePath) {
		super(address, secure, certificatePath);
	}

	@Override
	public String getAddress() {
		String address = super.getAddress();
		if (address == null) {
			address = getDefaultAddress();
		}
		return address.startsWith(UNIX_SOCKET_PREFIX) ? address.substring(UNIX_SOCKET_PREFIX.length()) : address;
	}

	public boolean isRemote() {
		return getAddress().startsWith("http") || getAddress().startsWith("tcp");
	}

	public boolean isLocalFileReference() {
		try {
			return Files.exists(Paths.get(getAddress()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Create a new {@link ResolvedDockerHost} from the given host configuration.
	 * @param connectionConfiguration the host configuration or {@code null}
	 * @return the resolved docker host
	 */
	public static ResolvedDockerHost from(@Nullable DockerConnectionConfiguration connectionConfiguration) {
		return from(Environment.SYSTEM, connectionConfiguration);
	}

	static ResolvedDockerHost from(Environment environment,
			@Nullable DockerConnectionConfiguration connectionConfiguration) {
		DockerConfigurationMetadata environmentConfiguration = DockerConfigurationMetadata.from(environment);
		if (environment.get(DOCKER_CONTEXT) != null) {
			DockerContext context = environmentConfiguration.forContext(environment.get(DOCKER_CONTEXT));
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		if (connectionConfiguration instanceof DockerConnectionConfiguration.Context contextConfiguration) {
			DockerContext context = environmentConfiguration.forContext(contextConfiguration.context());
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		if (environment.get(DOCKER_HOST) != null) {
			return new ResolvedDockerHost(environment.get(DOCKER_HOST), isTrue(environment.get(DOCKER_TLS_VERIFY)),
					environment.get(DOCKER_CERT_PATH));
		}
		if (connectionConfiguration instanceof DockerConnectionConfiguration.Host addressConfiguration) {
			return new ResolvedDockerHost(addressConfiguration.address(), addressConfiguration.secure(),
					addressConfiguration.certificatePath());
		}
		if (environmentConfiguration.getContext().getDockerHost() != null) {
			DockerContext context = environmentConfiguration.getContext();
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		return new ResolvedDockerHost(getDefaultAddress());
	}

	private static String getDefaultAddress() {
		return Platform.isWindows() ? WINDOWS_NAMED_PIPE_PATH : DOMAIN_SOCKET_PATH;
	}

	private static boolean isTrue(@Nullable String value) {
		try {
			return (value != null) && (Integer.parseInt(value) == 1);
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

}
