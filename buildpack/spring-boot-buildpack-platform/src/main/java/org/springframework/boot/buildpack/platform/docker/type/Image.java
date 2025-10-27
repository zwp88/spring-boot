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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import org.springframework.boot.buildpack.platform.json.MappedObject;
import org.springframework.util.StringUtils;

/**
 * Image details as returned from {@code Docker inspect}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class Image extends MappedObject {

	private final List<String> digests;

	private final ImageConfig config;

	private final List<LayerId> layers;

	private final @Nullable String os;

	private final @Nullable String architecture;

	private final @Nullable String variant;

	private final @Nullable String created;

	Image(JsonNode node) {
		super(node, MethodHandles.lookup());
		this.digests = childrenAt("/RepoDigests", JsonNode::asString);
		this.config = new ImageConfig(getNode().at("/Config"));
		this.layers = extractLayers(valueAt("/RootFS/Layers", String[].class));
		this.os = valueAt("/Os", String.class);
		this.architecture = valueAt("/Architecture", String.class);
		this.variant = valueAt("/Variant", String.class);
		this.created = valueAt("/Created", String.class);
	}

	private List<LayerId> extractLayers(String @Nullable [] layers) {
		if (layers == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(layers).map(LayerId::of).toList();
	}

	/**
	 * Return the digests of the image.
	 * @return the image digests
	 */
	public List<String> getDigests() {
		return this.digests;
	}

	/**
	 * Return image config information.
	 * @return the image config
	 */
	public ImageConfig getConfig() {
		return this.config;
	}

	/**
	 * Return the layer IDs contained in the image.
	 * @return the layer IDs.
	 */
	public List<LayerId> getLayers() {
		return this.layers;
	}

	/**
	 * Return the OS of the image.
	 * @return the image OS
	 */
	public String getOs() {
		return (StringUtils.hasText(this.os)) ? this.os : "linux";
	}

	/**
	 * Return the architecture of the image.
	 * @return the image architecture
	 */
	public @Nullable String getArchitecture() {
		return this.architecture;
	}

	/**
	 * Return the variant of the image.
	 * @return the image variant
	 */
	public @Nullable String getVariant() {
		return this.variant;
	}

	/**
	 * Return the created date of the image.
	 * @return the image created date
	 */
	public @Nullable String getCreated() {
		return this.created;
	}

	/**
	 * Create a new {@link Image} instance from the specified JSON content.
	 * @param content the JSON content
	 * @return a new {@link Image} instance
	 * @throws IOException on IO error
	 */
	public static Image of(InputStream content) throws IOException {
		return of(content, Image::new);
	}

}
