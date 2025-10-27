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

package org.springframework.boot.docker.compose.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeVersion;
import org.springframework.boot.docker.compose.core.DockerCliCommand.Type;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.log.LogMessage;
import org.springframework.util.CollectionUtils;

/**
 * Wrapper around {@code docker} and {@code docker-compose} command line tools.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCli {

	private static final Map<@Nullable File, DockerCommands> dockerCommandsCache = new HashMap<>();

	private static final Log logger = LogFactory.getLog(DockerCli.class);

	private final ProcessRunner processRunner;

	private final DockerCommands dockerCommands;

	private final DockerComposeOptions dockerComposeOptions;

	private final ComposeVersion composeVersion;

	/**
	 * Create a new {@link DockerCli} instance.
	 * @param workingDirectory the working directory or {@code null}
	 * @param dockerComposeOptions the Docker Compose options to use or {@code null}.
	 */
	DockerCli(@Nullable File workingDirectory, @Nullable DockerComposeOptions dockerComposeOptions) {
		this.processRunner = new ProcessRunner(workingDirectory);
		this.dockerCommands = dockerCommandsCache.computeIfAbsent(workingDirectory,
				(key) -> new DockerCommands(this.processRunner));
		this.dockerComposeOptions = (dockerComposeOptions != null) ? dockerComposeOptions : DockerComposeOptions.none();
		this.composeVersion = ComposeVersion.of(this.dockerCommands.get(Type.DOCKER_COMPOSE).version());
	}

	/**
	 * Run the given {@link DockerCli} command and return the response.
	 * @param <R> the response type
	 * @param dockerCommand the command to run
	 * @return the response
	 */
	<R> R run(DockerCliCommand<R> dockerCommand) {
		List<String> command = createCommand(dockerCommand.getType());
		command.addAll(dockerCommand.getCommand(this.composeVersion));
		Consumer<String> outputConsumer = createOutputConsumer(dockerCommand.getLogLevel());
		String json = this.processRunner.run(outputConsumer, command.toArray(new String[0]));
		return dockerCommand.deserialize(json);
	}

	private @Nullable Consumer<String> createOutputConsumer(@Nullable LogLevel logLevel) {
		if (logLevel == null || logLevel == LogLevel.OFF) {
			return null;
		}
		return (line) -> logLevel.log(logger, line);
	}

	private List<String> createCommand(Type type) {
		return switch (type) {
			case DOCKER -> new ArrayList<>(this.dockerCommands.get(type).command());
			case DOCKER_COMPOSE -> {
				List<String> result = new ArrayList<>(this.dockerCommands.get(type).command());
				DockerComposeFile composeFile = this.dockerComposeOptions.composeFile();
				if (composeFile != null) {
					for (File file : composeFile.getFiles()) {
						result.add("--file");
						result.add(file.getPath());
					}
				}
				result.add("--ansi");
				result.add("never");
				Set<String> activeProfiles = this.dockerComposeOptions.activeProfiles();
				if (!CollectionUtils.isEmpty(activeProfiles)) {
					for (String profile : activeProfiles) {
						result.add("--profile");
						result.add(profile);
					}
				}
				List<String> arguments = this.dockerComposeOptions.arguments();
				if (!CollectionUtils.isEmpty(arguments)) {
					result.addAll(arguments);
				}
				yield result;
			}
		};
	}

	/**
	 * Return the {@link DockerComposeFile} being used by this CLI instance.
	 * @return the Docker Compose file
	 */
	@Nullable DockerComposeFile getDockerComposeFile() {
		return this.dockerComposeOptions.composeFile();
	}

	/**
	 * Holds details of the actual CLI commands to invoke.
	 */
	private static class DockerCommands {

		private final DockerCommand dockerCommand;

		private final DockerCommand dockerComposeCommand;

		DockerCommands(ProcessRunner processRunner) {
			this.dockerCommand = getDockerCommand(processRunner);
			this.dockerComposeCommand = getDockerComposeCommand(processRunner);
		}

		private DockerCommand getDockerCommand(ProcessRunner processRunner) {
			try {
				String version = processRunner.run("docker", "version", "--format", "{{.Client.Version}}");
				logger.trace(LogMessage.format("Using docker %s", version));
				return new DockerCommand(version, List.of("docker"));
			}
			catch (ProcessStartException ex) {
				throw new DockerProcessStartException("Unable to start docker process. Is docker correctly installed?",
						ex);
			}
			catch (ProcessExitException ex) {
				if (ex.getStdErr().contains("docker daemon is not running")
						|| ex.getStdErr().contains("Cannot connect to the Docker daemon")) {
					throw new DockerNotRunningException(ex.getStdErr(), ex);
				}
				throw ex;
			}
		}

		private DockerCommand getDockerComposeCommand(ProcessRunner processRunner) {
			try {
				DockerCliComposeVersionResponse response = DockerJson.deserialize(
						processRunner.run("docker", "compose", "version", "--format", "json"),
						DockerCliComposeVersionResponse.class);
				logger.trace(LogMessage.format("Using Docker Compose %s", response.version()));
				return new DockerCommand(response.version(), List.of("docker", "compose"));
			}
			catch (ProcessExitException ex) {
				// Ignore and try docker-compose
			}
			try {
				DockerCliComposeVersionResponse response = DockerJson.deserialize(
						processRunner.run("docker-compose", "version", "--format", "json"),
						DockerCliComposeVersionResponse.class);
				logger.trace(LogMessage.format("Using docker-compose %s", response.version()));
				return new DockerCommand(response.version(), List.of("docker-compose"));
			}
			catch (ProcessStartException ex) {
				throw new DockerProcessStartException(
						"Unable to start 'docker-compose' process or use 'docker compose'. Is docker correctly installed?",
						ex);
			}
		}

		DockerCommand get(Type type) {
			return switch (type) {
				case DOCKER -> this.dockerCommand;
				case DOCKER_COMPOSE -> this.dockerComposeCommand;
			};
		}

	}

	private record DockerCommand(String version, List<String> command) {

	}

	/**
	 * Options for Docker Compose.
	 *
	 * @param composeFile the Docker Compose file to use
	 * @param activeProfiles the profiles to activate
	 * @param arguments the arguments to pass to Docker Compose
	 */
	record DockerComposeOptions(@Nullable DockerComposeFile composeFile, Set<String> activeProfiles,
			List<String> arguments) {

		DockerComposeOptions(@Nullable DockerComposeFile composeFile, @Nullable Set<String> activeProfiles,
				@Nullable List<String> arguments) {
			this.composeFile = composeFile;
			this.activeProfiles = (activeProfiles != null) ? activeProfiles : Collections.emptySet();
			this.arguments = (arguments != null) ? arguments : Collections.emptyList();
		}

		static DockerComposeOptions none() {
			return new DockerComposeOptions(null, null, null);
		}

	}

}
