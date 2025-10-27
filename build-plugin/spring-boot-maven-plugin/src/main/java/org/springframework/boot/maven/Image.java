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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.maven.artifact.Artifact;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.BuildpackReference;
import org.springframework.boot.buildpack.platform.build.Cache;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Image configuration options.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 * @author Julian Liebig
 * @since 2.3.0
 */
public class Image {

	@Nullable String name;

	@Nullable String builder;

	@Nullable Boolean trustBuilder;

	@Nullable String runImage;

	@Nullable Map<String, String> env;

	@Nullable Boolean cleanCache;

	boolean verboseLogging;

	@Nullable PullPolicy pullPolicy;

	@Nullable Boolean publish;

	@Nullable List<String> buildpacks;

	@Nullable List<String> bindings;

	@Nullable String network;

	@Nullable List<String> tags;

	@Nullable CacheInfo buildWorkspace;

	@Nullable CacheInfo buildCache;

	@Nullable CacheInfo launchCache;

	@Nullable String createdDate;

	@Nullable String applicationDirectory;

	@Nullable List<String> securityOptions;

	@Nullable String imagePlatform;

	/**
	 * The name of the created image.
	 * @return the image name
	 */
	public @Nullable String getName() {
		return this.name;
	}

	void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * The name of the builder image to use to create the image.
	 * @return the builder image name
	 */
	public @Nullable String getBuilder() {
		return this.builder;
	}

	void setBuilder(@Nullable String builder) {
		this.builder = builder;
	}

	/**
	 * If the builder should be treated as trusted.
	 * @return {@code true} if the builder should be treated as trusted
	 */
	public @Nullable Boolean getTrustBuilder() {
		return this.trustBuilder;
	}

	void setTrustBuilder(@Nullable Boolean trustBuilder) {
		this.trustBuilder = trustBuilder;
	}

	/**
	 * The name of the run image to use to create the image.
	 * @return the builder image name
	 */
	public @Nullable String getRunImage() {
		return this.runImage;
	}

	void setRunImage(@Nullable String runImage) {
		this.runImage = runImage;
	}

	/**
	 * Environment properties that should be passed to the builder.
	 * @return the environment properties
	 */
	public @Nullable Map<String, String> getEnv() {
		return this.env;
	}

	/**
	 * If the cache should be cleaned before building.
	 * @return {@code true} if the cache should be cleaned
	 */
	public @Nullable Boolean getCleanCache() {
		return this.cleanCache;
	}

	void setCleanCache(@Nullable Boolean cleanCache) {
		this.cleanCache = cleanCache;
	}

	/**
	 * If verbose logging is required.
	 * @return {@code true} for verbose logging
	 */
	public boolean isVerboseLogging() {
		return this.verboseLogging;
	}

	/**
	 * If images should be pulled from a remote repository during image build.
	 * @return the pull policy
	 */
	public @Nullable PullPolicy getPullPolicy() {
		return this.pullPolicy;
	}

	void setPullPolicy(@Nullable PullPolicy pullPolicy) {
		this.pullPolicy = pullPolicy;
	}

	/**
	 * If the built image should be pushed to a registry.
	 * @return {@code true} if the image should be published
	 */
	public @Nullable Boolean getPublish() {
		return this.publish;
	}

	void setPublish(@Nullable Boolean publish) {
		this.publish = publish;
	}

	/**
	 * Returns the network the build container will connect to.
	 * @return the network
	 */
	public @Nullable String getNetwork() {
		return this.network;
	}

	public void setNetwork(@Nullable String network) {
		this.network = network;
	}

	/**
	 * Returns the created date for the image.
	 * @return the created date
	 */
	public @Nullable String getCreatedDate() {
		return this.createdDate;
	}

	public void setCreatedDate(@Nullable String createdDate) {
		this.createdDate = createdDate;
	}

	/**
	 * Returns the application content directory for the image.
	 * @return the application directory
	 */
	public @Nullable String getApplicationDirectory() {
		return this.applicationDirectory;
	}

	public void setApplicationDirectory(@Nullable String applicationDirectory) {
		this.applicationDirectory = applicationDirectory;
	}

	/**
	 * Returns the platform (os/architecture/variant) that will be used for all pulled
	 * images. When {@code null}, the system will choose a platform based on the host
	 * operating system and architecture.
	 * @return the image platform
	 */
	public @Nullable String getImagePlatform() {
		return this.imagePlatform;
	}

	public void setImagePlatform(@Nullable String imagePlatform) {
		this.imagePlatform = imagePlatform;
	}

	BuildRequest getBuildRequest(Artifact artifact, Function<Owner, TarArchive> applicationContent) {
		return customize(BuildRequest.of(getOrDeduceName(artifact), applicationContent));
	}

	private ImageReference getOrDeduceName(Artifact artifact) {
		if (StringUtils.hasText(this.name)) {
			return ImageReference.of(this.name);
		}
		ImageName imageName = ImageName.of(artifact.getArtifactId());
		return ImageReference.of(imageName, artifact.getVersion());
	}

	private BuildRequest customize(BuildRequest request) {
		if (StringUtils.hasText(this.builder)) {
			request = request.withBuilder(ImageReference.of(this.builder));
		}
		if (this.trustBuilder != null) {
			request = request.withTrustBuilder(this.trustBuilder);
		}
		if (StringUtils.hasText(this.runImage)) {
			request = request.withRunImage(ImageReference.of(this.runImage));
		}
		if (!CollectionUtils.isEmpty(this.env)) {
			request = request.withEnv(this.env);
		}
		if (this.cleanCache != null) {
			request = request.withCleanCache(this.cleanCache);
		}
		request = request.withVerboseLogging(this.verboseLogging);
		if (this.pullPolicy != null) {
			request = request.withPullPolicy(this.pullPolicy);
		}
		if (this.publish != null) {
			request = request.withPublish(this.publish);
		}
		if (!CollectionUtils.isEmpty(this.buildpacks)) {
			request = request.withBuildpacks(this.buildpacks.stream().map(BuildpackReference::of).toList());
		}
		if (!CollectionUtils.isEmpty(this.bindings)) {
			request = request.withBindings(this.bindings.stream().map(Binding::of).toList());
		}
		request = request.withNetwork(this.network);
		if (!CollectionUtils.isEmpty(this.tags)) {
			request = request.withTags(this.tags.stream().map(ImageReference::of).toList());
		}
		if (this.buildWorkspace != null) {
			Cache cache = this.buildWorkspace.asCache();
			Assert.state(cache != null, "'cache' must not be null");
			request = request.withBuildWorkspace(cache);
		}
		if (this.buildCache != null) {
			Cache cache = this.buildCache.asCache();
			Assert.state(cache != null, "'cache' must not be null");
			request = request.withBuildCache(cache);
		}
		if (this.launchCache != null) {
			Cache cache = this.launchCache.asCache();
			Assert.state(cache != null, "'cache' must not be null");
			request = request.withLaunchCache(cache);
		}
		if (StringUtils.hasText(this.createdDate)) {
			request = request.withCreatedDate(this.createdDate);
		}
		if (StringUtils.hasText(this.applicationDirectory)) {
			request = request.withApplicationDirectory(this.applicationDirectory);
		}
		if (this.securityOptions != null) {
			request = request.withSecurityOptions(this.securityOptions);
		}
		if (StringUtils.hasText(this.imagePlatform)) {
			request = request.withImagePlatform(this.imagePlatform);
		}
		return request;
	}

}
