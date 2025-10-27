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

package org.springframework.boot.configurationprocessor;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.name.ConstructorParameterNameAnnotationProperties;
import org.springframework.boot.configurationsample.name.JavaBeanNameAnnotationProperties;
import org.springframework.boot.configurationsample.name.LombokNameAnnotationProperties;
import org.springframework.boot.configurationsample.name.RecordComponentNameAnnotationProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for using {@code @Name}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class NameAnnotationPropertiesTests extends AbstractMetadataGenerationTests {

	@Test
	void constructorParameterNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(ConstructorParameterNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(ConstructorParameterNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(ConstructorParameterNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void recordComponentNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(RecordComponentNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(RecordComponentNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(RecordComponentNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void javaBeanNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(JavaBeanNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(JavaBeanNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(JavaBeanNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void lombokNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(LombokNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(LombokNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(LombokNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

}
