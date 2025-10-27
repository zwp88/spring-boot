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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link VolumeName}.
 *
 * @author Phillip Webb
 */
class VolumeNameTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void randomWhenPrefixIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.random(null))
			.withMessage("'prefix' must not be null");
	}

	@Test
	void randomGeneratesRandomString() {
		VolumeName v1 = VolumeName.random("abc-");
		VolumeName v2 = VolumeName.random("abc-");
		assertThat(v1.toString()).startsWith("abc-").hasSize(14);
		assertThat(v2.toString()).startsWith("abc-").hasSize(14);
		assertThat(v1).isNotEqualTo(v2);
		assertThat(v1.toString()).isNotEqualTo(v2.toString());
	}

	@Test
	void randomStringWithLengthGeneratesRandomString() {
		VolumeName v1 = VolumeName.random("abc-", 20);
		VolumeName v2 = VolumeName.random("abc-", 20);
		assertThat(v1.toString()).startsWith("abc-").hasSize(24);
		assertThat(v2.toString()).startsWith("abc-").hasSize(24);
		assertThat(v1).isNotEqualTo(v2);
		assertThat(v1.toString()).isNotEqualTo(v2.toString());
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void basedOnWhenSourceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn(null, "prefix", "suffix", 6))
			.withMessage("'source' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void basedOnWhenNameExtractorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("test", null, "prefix", "suffix", 6))
			.withMessage("'nameExtractor' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void basedOnWhenPrefixIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("test", null, "suffix", 6))
			.withMessage("'prefix' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void basedOnWhenSuffixIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("test", "prefix", null, 6))
			.withMessage("'suffix' must not be null");
	}

	@Test
	void basedOnGeneratesHashBasedName() {
		VolumeName name = VolumeName.basedOn("index.docker.io/library/myapp:latest", "pack-cache-", ".build", 6);
		assertThat(name).hasToString("pack-cache-40a311b545d7.build");
	}

	@Test
	void basedOnWhenSizeIsTooBigThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.basedOn("name", "prefix", "suffix", 33))
			.withMessage("'digestLength' must be less than or equal to 32");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenValueIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> VolumeName.of(null))
			.withMessage("'value' must not be null");
	}

	@Test
	void ofGeneratesValue() {
		VolumeName name = VolumeName.of("test");
		assertThat(name).hasToString("test");
	}

	@Test
	void equalsAndHashCode() {
		VolumeName n1 = VolumeName.of("test1");
		VolumeName n2 = VolumeName.of("test1");
		VolumeName n3 = VolumeName.of("test2");
		assertThat(n1).hasSameHashCodeAs(n2);
		assertThat(n1).isEqualTo(n1).isEqualTo(n2).isNotEqualTo(n3);
	}

}
