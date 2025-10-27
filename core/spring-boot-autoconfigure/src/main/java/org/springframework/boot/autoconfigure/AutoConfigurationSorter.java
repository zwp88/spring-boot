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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;

/**
 * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
 * reading {@link AutoConfigureOrder @AutoConfigureOrder},
 * {@link AutoConfigureBefore @AutoConfigureBefore} and
 * {@link AutoConfigureAfter @AutoConfigureAfter} annotations (without loading classes).
 *
 * @author Phillip Webb
 */
class AutoConfigurationSorter {

	private final MetadataReaderFactory metadataReaderFactory;

	private final @Nullable AutoConfigurationMetadata autoConfigurationMetadata;

	private final @Nullable UnaryOperator<String> replacementMapper;

	AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
			@Nullable AutoConfigurationMetadata autoConfigurationMetadata,
			@Nullable UnaryOperator<String> replacementMapper) {
		Assert.notNull(metadataReaderFactory, "'metadataReaderFactory' must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.autoConfigurationMetadata = autoConfigurationMetadata;
		this.replacementMapper = replacementMapper;
	}

	List<String> getInPriorityOrder(Collection<String> classNames) {
		// Initially sort alphabetically
		List<String> alphabeticallyOrderedClassNames = new ArrayList<>(classNames);
		Collections.sort(alphabeticallyOrderedClassNames);
		// Then sort by order
		AutoConfigurationClasses classes = new AutoConfigurationClasses(this.metadataReaderFactory,
				this.autoConfigurationMetadata, alphabeticallyOrderedClassNames);
		List<String> orderedClassNames = new ArrayList<>(classNames);
		Collections.sort(orderedClassNames);
		orderedClassNames.sort((o1, o2) -> {
			int i1 = classes.get(o1).getOrder();
			int i2 = classes.get(o2).getOrder();
			return Integer.compare(i1, i2);
		});
		// Then respect @AutoConfigureBefore @AutoConfigureAfter
		orderedClassNames = sortByAnnotation(classes, orderedClassNames);
		return orderedClassNames;
	}

	private List<String> sortByAnnotation(AutoConfigurationClasses classes, List<String> classNames) {
		List<String> toSort = new ArrayList<>(classNames);
		toSort.addAll(classes.getAllNames());
		Set<String> sorted = new LinkedHashSet<>();
		Set<String> processing = new LinkedHashSet<>();
		while (!toSort.isEmpty()) {
			doSortByAfterAnnotation(classes, toSort, sorted, processing, null);
		}
		sorted.retainAll(classNames);
		return new ArrayList<>(sorted);
	}

	private void doSortByAfterAnnotation(AutoConfigurationClasses classes, List<String> toSort, Set<String> sorted,
			Set<String> processing, @Nullable String current) {
		if (current == null) {
			current = toSort.remove(0);
		}
		processing.add(current);
		Set<String> afters = new TreeSet<>(Comparator.comparing(toSort::indexOf));
		afters.addAll(classes.getClassesRequestedAfter(current));
		for (String after : afters) {
			checkForCycles(processing, current, after);
			if (!sorted.contains(after) && toSort.contains(after)) {
				doSortByAfterAnnotation(classes, toSort, sorted, processing, after);
			}
		}
		processing.remove(current);
		sorted.add(current);
	}

	private void checkForCycles(Set<String> processing, String current, String after) {
		Assert.state(!processing.contains(after),
				() -> "AutoConfigure cycle detected between " + current + " and " + after);
	}

	private class AutoConfigurationClasses {

		private final Map<String, AutoConfigurationClass> classes = new LinkedHashMap<>();

		AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
				@Nullable AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames) {
			addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, true);
		}

		Set<String> getAllNames() {
			return this.classes.keySet();
		}

		private void addToClasses(MetadataReaderFactory metadataReaderFactory,
				@Nullable AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames,
				boolean required) {
			for (String className : classNames) {
				if (!this.classes.containsKey(className)) {
					AutoConfigurationClass autoConfigurationClass = new AutoConfigurationClass(className,
							metadataReaderFactory, autoConfigurationMetadata);
					boolean available = autoConfigurationClass.isAvailable();
					if (required || available) {
						this.classes.put(className, autoConfigurationClass);
					}
					if (available) {
						addToClasses(metadataReaderFactory, autoConfigurationMetadata,
								autoConfigurationClass.getBefore(), false);
						addToClasses(metadataReaderFactory, autoConfigurationMetadata,
								autoConfigurationClass.getAfter(), false);
					}
				}
			}
		}

		AutoConfigurationClass get(String className) {
			AutoConfigurationClass autoConfigurationClass = this.classes.get(className);
			Assert.state(autoConfigurationClass != null, "'autoConfigurationClass' must not be null");
			return autoConfigurationClass;
		}

		Set<String> getClassesRequestedAfter(String className) {
			Set<String> classesRequestedAfter = new LinkedHashSet<>(get(className).getAfter());
			this.classes.forEach((name, autoConfigurationClass) -> {
				if (autoConfigurationClass.getBefore().contains(className)) {
					classesRequestedAfter.add(name);
				}
			});
			return classesRequestedAfter;
		}

	}

	private class AutoConfigurationClass {

		private final String className;

		private final MetadataReaderFactory metadataReaderFactory;

		private final @Nullable AutoConfigurationMetadata autoConfigurationMetadata;

		private volatile @Nullable AnnotationMetadata annotationMetadata;

		private volatile @Nullable Set<String> before;

		private volatile @Nullable Set<String> after;

		AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
				@Nullable AutoConfigurationMetadata autoConfigurationMetadata) {
			this.className = className;
			this.metadataReaderFactory = metadataReaderFactory;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
		}

		boolean isAvailable() {
			try {
				if (!wasProcessed()) {
					getAnnotationMetadata();
				}
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

		Set<String> getBefore() {
			Set<String> before = this.before;
			if (before == null) {
				before = getClassNames("AutoConfigureBefore", AutoConfigureBefore.class);
				this.before = before;
			}
			return before;
		}

		Set<String> getAfter() {
			Set<String> after = this.after;
			if (after == null) {
				after = getClassNames("AutoConfigureAfter", AutoConfigureAfter.class);
				this.after = after;
			}
			return after;
		}

		private Set<String> getClassNames(String metadataKey, Class<? extends Annotation> annotation) {
			Set<String> annotationValue = wasProcessed() ? getSet(metadataKey) : getAnnotationValue(annotation);
			return applyReplacements(annotationValue);
		}

		private Set<String> getSet(String metadataKey) {
			Assert.state(this.autoConfigurationMetadata != null, "'autoConfigurationMetadata' must not be null");
			return this.autoConfigurationMetadata.getSet(this.className, metadataKey, Collections.emptySet());
		}

		private Set<String> applyReplacements(Set<String> values) {
			if (AutoConfigurationSorter.this.replacementMapper == null) {
				return values;
			}
			Set<String> replaced = new LinkedHashSet<>(values);
			for (String value : values) {
				replaced.add(AutoConfigurationSorter.this.replacementMapper.apply(value));
			}
			return replaced;
		}

		private int getOrder() {
			if (wasProcessed()) {
				Assert.state(this.autoConfigurationMetadata != null, "'autoConfigurationMetadata' must not be null");
				return this.autoConfigurationMetadata.getInteger(this.className, "AutoConfigureOrder",
						AutoConfigureOrder.DEFAULT_ORDER);
			}
			Map<String, @Nullable Object> attributes = getAnnotationMetadata()
				.getAnnotationAttributes(AutoConfigureOrder.class.getName());
			if (attributes != null) {
				Integer value = (Integer) attributes.get("value");
				Assert.state(value != null, "'value' must not be null");
				return value;
			}
			return AutoConfigureOrder.DEFAULT_ORDER;
		}

		private boolean wasProcessed() {
			return (this.autoConfigurationMetadata != null
					&& this.autoConfigurationMetadata.wasProcessed(this.className));
		}

		private Set<String> getAnnotationValue(Class<?> annotation) {
			Map<String, @Nullable Object> attributes = getAnnotationMetadata()
				.getAnnotationAttributes(annotation.getName(), true);
			if (attributes == null) {
				return Collections.emptySet();
			}
			Set<String> result = new LinkedHashSet<>();
			String[] value = (String[]) attributes.get("value");
			String[] name = (String[]) attributes.get("name");
			Assert.state(value != null, "'value' must not be null");
			Assert.state(name != null, "'name' must not be null");
			Collections.addAll(result, value);
			Collections.addAll(result, name);
			return result;
		}

		private AnnotationMetadata getAnnotationMetadata() {
			AnnotationMetadata annotationMetadata = this.annotationMetadata;
			if (annotationMetadata == null) {
				try {
					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(this.className);
					annotationMetadata = metadataReader.getAnnotationMetadata();
					this.annotationMetadata = annotationMetadata;
				}
				catch (IOException ex) {
					throw new IllegalStateException("Unable to read meta-data for class " + this.className, ex);
				}
			}
			return annotationMetadata;
		}

	}

}
