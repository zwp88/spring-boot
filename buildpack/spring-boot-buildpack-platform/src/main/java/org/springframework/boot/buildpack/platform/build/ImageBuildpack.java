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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.build.BuildpackLayersMetadata.BuildpackLayerDetails;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.docker.type.LayerId;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * A {@link Buildpack} that references a buildpack contained in an OCI image.
 *
 * The reference must be an OCI image reference. The reference can optionally contain a
 * prefix {@code docker://} to unambiguously identify it as an image buildpack reference.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class ImageBuildpack implements Buildpack {

	private static final String PREFIX = "docker://";

	private final BuildpackCoordinates coordinates;

	private final @Nullable ExportedLayers exportedLayers;

	private ImageBuildpack(BuildpackResolverContext context, ImageReference imageReference) {
		ImageReference reference = imageReference.inTaggedOrDigestForm();
		try {
			Image image = context.fetchImage(reference, ImageType.BUILDPACK);
			BuildpackMetadata buildpackMetadata = BuildpackMetadata.fromImage(image);
			this.coordinates = BuildpackCoordinates.fromBuildpackMetadata(buildpackMetadata);
			this.exportedLayers = (!buildpackExistsInBuilder(context, image.getLayers()))
					? new ExportedLayers(context, reference) : null;
		}
		catch (IOException | DockerEngineException ex) {
			throw new IllegalArgumentException("Error pulling buildpack image '" + reference + "'", ex);
		}
	}

	private boolean buildpackExistsInBuilder(BuildpackResolverContext context, List<LayerId> imageLayers) {
		BuildpackLayerDetails buildpackLayerDetails = context.getBuildpackLayersMetadata()
			.getBuildpack(this.coordinates.getId(), this.coordinates.getVersion());
		String layerDiffId = (buildpackLayerDetails != null) ? buildpackLayerDetails.getLayerDiffId() : null;
		return (layerDiffId != null) && imageLayers.stream().map(LayerId::toString).anyMatch(layerDiffId::equals);
	}

	@Override
	public BuildpackCoordinates getCoordinates() {
		return this.coordinates;
	}

	@Override
	public void apply(IOConsumer<Layer> layers) throws IOException {
		if (this.exportedLayers != null) {
			this.exportedLayers.apply(layers);
		}
	}

	/**
	 * A {@link BuildpackResolver} compatible method to resolve image buildpacks.
	 * @param context the resolver context
	 * @param reference the buildpack reference
	 * @return the resolved {@link Buildpack} or {@code null}
	 */
	static @Nullable Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
		boolean unambiguous = reference.hasPrefix(PREFIX);
		try {
			ImageReference imageReference;
			if (unambiguous) {
				String subReference = reference.getSubReference(PREFIX);
				Assert.state(subReference != null, "'subReference' must not be null");
				imageReference = ImageReference.of(subReference);
			}
			else {
				imageReference = ImageReference.of(reference.toString());
			}
			return new ImageBuildpack(context, imageReference);
		}
		catch (IllegalArgumentException ex) {
			if (unambiguous) {
				throw ex;
			}
			return null;
		}
	}

	private static class ExportedLayers {

		private final List<Path> layerFiles;

		ExportedLayers(BuildpackResolverContext context, ImageReference imageReference) throws IOException {
			List<Path> layerFiles = new ArrayList<>();
			context.exportImageLayers(imageReference,
					(name, tarArchive) -> layerFiles.add(createLayerFile(tarArchive)));
			this.layerFiles = Collections.unmodifiableList(layerFiles);
		}

		private Path createLayerFile(TarArchive tarArchive) throws IOException {
			Path sourceTarFile = Files.createTempFile("create-builder-scratch-source-", null);
			try (OutputStream out = Files.newOutputStream(sourceTarFile)) {
				tarArchive.writeTo(out);
			}
			Path layerFile = Files.createTempFile("create-builder-scratch-", null);
			try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(layerFile))) {
				try (TarArchiveInputStream in = new TarArchiveInputStream(Files.newInputStream(sourceTarFile))) {
					out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
					TarArchiveEntry entry = in.getNextEntry();
					while (entry != null) {
						out.putArchiveEntry(entry);
						StreamUtils.copy(in, out);
						out.closeArchiveEntry();
						entry = in.getNextEntry();
					}
					out.finish();
				}
			}
			return layerFile;
		}

		void apply(IOConsumer<Layer> layers) throws IOException {
			for (Path path : this.layerFiles) {
				layers.accept(Layer.fromTarArchive((out) -> {
					InputStream in = Files.newInputStream(path);
					StreamUtils.copy(in, out);
				}));
				Files.delete(path);
			}
		}

	}

}
