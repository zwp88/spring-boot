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

package org.springframework.boot.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ContextResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Class can be used to obtain {@link ResourceLoader ResourceLoaders} supporting
 * additional {@link ProtocolResolver ProtocolResolvers} registered in
 * {@code spring.factories}.
 * <p>
 * When not delegating to an existing resource loader, plain paths without a qualifier
 * will resolve to file system resources. This is different from
 * {@code DefaultResourceLoader}, which resolves unqualified paths to classpath resources.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.3.0
 */
public class ApplicationResourceLoader extends DefaultResourceLoader {

	@Override
	protected Resource getResourceByPath(String path) {
		return new ApplicationResource(path);
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}. The factories file will
	 * be resolved using the default class loader at the time this call is made. Resources
	 * will be resolved using the default class loader at the time they are resolved.
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get() {
		return get((ClassLoader) null);
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}. The factories files and
	 * resources will be resolved using the specified class loader.
	 * @param classLoader the class loader to use or {@code null} to use the default class
	 * loader
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(@Nullable ClassLoader classLoader) {
		return get(classLoader, SpringFactoriesLoader.forDefaultResourceLocation(classLoader));
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}.
	 * @param classLoader the class loader to use or {@code null} to use the default class
	 * loader
	 * @param springFactoriesLoader the {@link SpringFactoriesLoader} used to load
	 * {@link ProtocolResolver ProtocolResolvers}
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(@Nullable ClassLoader classLoader, SpringFactoriesLoader springFactoriesLoader) {
		return get(classLoader, springFactoriesLoader, null);
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}.
	 * @param classLoader the class loader to use or {@code null} to use the default class
	 * loader
	 * @param springFactoriesLoader the {@link SpringFactoriesLoader} used to load
	 * {@link ProtocolResolver ProtocolResolvers}
	 * @param workingDirectory the working directory
	 * @return a {@link ResourceLoader} instance
	 * @since 3.5.0
	 */
	public static ResourceLoader get(@Nullable ClassLoader classLoader, SpringFactoriesLoader springFactoriesLoader,
			@Nullable Path workingDirectory) {
		return get(ApplicationFileSystemResourceLoader.get(classLoader, workingDirectory), springFactoriesLoader);
	}

	/**
	 * Return a {@link ResourceLoader} delegating to the given resource loader and
	 * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
	 * {@code spring.factories}. The factories file will be resolved using the default
	 * class loader at the time this call is made.
	 * @param resourceLoader the delegate resource loader
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(ResourceLoader resourceLoader) {
		return get(resourceLoader, false);
	}

	/**
	 * Return a {@link ResourceLoader} delegating to the given resource loader and
	 * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
	 * {@code spring.factories}. The factories file will be resolved using the default
	 * class loader at the time this call is made.
	 * @param resourceLoader the delegate resource loader
	 * @param preferFileResolution if file based resolution is preferred when a suitable
	 * {@link FilePathResolver} support the resource
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.1
	 */
	public static ResourceLoader get(ResourceLoader resourceLoader, boolean preferFileResolution) {
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		return get(resourceLoader, SpringFactoriesLoader.forDefaultResourceLocation(resourceLoader.getClassLoader()),
				preferFileResolution);
	}

	/**
	 * Return a {@link ResourceLoader} delegating to the given resource loader and
	 * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
	 * {@code spring.factories}.
	 * @param resourceLoader the delegate resource loader
	 * @param springFactoriesLoader the {@link SpringFactoriesLoader} used to load
	 * {@link ProtocolResolver ProtocolResolvers}
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(ResourceLoader resourceLoader, SpringFactoriesLoader springFactoriesLoader) {
		return get(resourceLoader, springFactoriesLoader, false);
	}

	private static ResourceLoader get(ResourceLoader resourceLoader, SpringFactoriesLoader springFactoriesLoader,
			boolean preferFileResolution) {
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		Assert.notNull(springFactoriesLoader, "'springFactoriesLoader' must not be null");
		List<ProtocolResolver> protocolResolvers = springFactoriesLoader.load(ProtocolResolver.class);
		List<FilePathResolver> filePathResolvers = (preferFileResolution)
				? springFactoriesLoader.load(FilePathResolver.class) : Collections.emptyList();
		return new ProtocolResolvingResourceLoader(resourceLoader, protocolResolvers, filePathResolvers);
	}

	/**
	 * Internal {@link ResourceLoader} used to load {@link ApplicationResource}.
	 */
	private static final class ApplicationFileSystemResourceLoader extends DefaultResourceLoader {

		private static final ResourceLoader shared = new ApplicationFileSystemResourceLoader(null, null);

		private final @Nullable Path workingDirectory;

		private ApplicationFileSystemResourceLoader(@Nullable ClassLoader classLoader,
				@Nullable Path workingDirectory) {
			super(classLoader);
			this.workingDirectory = workingDirectory;
		}

		@Override
		public Resource getResource(String location) {
			Resource resource = super.getResource(location);
			if (this.workingDirectory == null) {
				return resource;
			}
			if (!resource.isFile()) {
				return resource;
			}
			return resolveFile(resource, this.workingDirectory);
		}

		private Resource resolveFile(Resource resource, Path workingDirectory) {
			try {
				File file = resource.getFile();
				return new ApplicationResource(workingDirectory.resolve(file.toPath()));
			}
			catch (FileNotFoundException ex) {
				return resource;
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		protected Resource getResourceByPath(String path) {
			return new ApplicationResource(path);
		}

		static ResourceLoader get(@Nullable ClassLoader classLoader, @Nullable Path workingDirectory) {
			if (classLoader == null && workingDirectory != null) {
				throw new IllegalArgumentException(
						"It's not possible to use null as 'classLoader' but specify a 'workingDirectory'");
			}
			return (classLoader != null) ? new ApplicationFileSystemResourceLoader(classLoader, workingDirectory)
					: ApplicationFileSystemResourceLoader.shared;
		}

	}

	/**
	 * Strategy interface registered in {@code spring.factories} and used by
	 * {@link ApplicationResourceLoader} to determine the file path of loaded resource
	 * when it can also be represented as a {@link FileSystemResource}.
	 *
	 * @author Phillip Webb
	 * @since 3.4.5
	 */
	public interface FilePathResolver {

		/**
		 * Return the {@code path} of the given resource if it can also be represented as
		 * a {@link FileSystemResource}.
		 * @param location the location used to create the resource
		 * @param resource the resource to check
		 * @return the file path of the resource or {@code null} if the it is not possible
		 * to represent the resource as a {@link FileSystemResource}.
		 */
		@Nullable String resolveFilePath(String location, Resource resource);

	}

	/**
	 * An application {@link Resource}.
	 */
	private static final class ApplicationResource extends FileSystemResource implements ContextResource {

		ApplicationResource(String path) {
			super(path);
		}

		ApplicationResource(Path path) {
			super(path);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

	}

	/**
	 * {@link ResourceLoader} decorator that adds support for additional
	 * {@link ProtocolResolver ProtocolResolvers}.
	 */
	private static class ProtocolResolvingResourceLoader implements ResourceLoader {

		private final ResourceLoader resourceLoader;

		private final List<ProtocolResolver> protocolResolvers;

		private final List<FilePathResolver> filePathResolvers;

		ProtocolResolvingResourceLoader(ResourceLoader resourceLoader, List<ProtocolResolver> protocolResolvers,
				List<FilePathResolver> filePathResolvers) {
			this.resourceLoader = resourceLoader;
			this.protocolResolvers = protocolResolvers;
			this.filePathResolvers = filePathResolvers;
		}

		@Override
		public @Nullable ClassLoader getClassLoader() {
			return this.resourceLoader.getClassLoader();
		}

		@Override
		public Resource getResource(String location) {
			if (StringUtils.hasLength(location)) {
				for (ProtocolResolver protocolResolver : this.protocolResolvers) {
					Resource resource = protocolResolver.resolve(location, this);
					if (resource != null) {
						return resource;
					}
				}
			}
			Resource resource = this.resourceLoader.getResource(location);
			String filePath = getFilePath(location, resource);
			return (filePath != null) ? new ApplicationResource(filePath) : resource;
		}

		private @Nullable String getFilePath(String location, Resource resource) {
			for (FilePathResolver filePathResolver : this.filePathResolvers) {
				String filePath = filePathResolver.resolveFilePath(location, resource);
				if (filePath != null) {
					return filePath;
				}
			}
			return null;
		}

	}

}
