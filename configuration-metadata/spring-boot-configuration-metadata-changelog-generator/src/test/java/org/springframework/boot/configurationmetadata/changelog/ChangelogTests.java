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

package org.springframework.boot.configurationmetadata.changelog;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Changelog}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Yoobin Yoon
 */
class ChangelogTests {

	@Test
	void diffContainsDifferencesBetweenLeftAndRightInputs() {
		Changelog differences = TestChangelog.load();
		assertThat(differences).isNotNull();
		assertThat(differences.oldVersionNumber()).isEqualTo("1.0");
		assertThat(differences.newVersionNumber()).isEqualTo("2.0");
		assertThat(differences.differences()).hasSize(7);
		List<Difference> added = differences.differences()
			.stream()
			.filter((difference) -> difference.type() == DifferenceType.ADDED)
			.toList();
		assertThat(added).hasSize(2)
			.anySatisfy((entry) -> assertProperty(entry.newProperty(), "test.add", String.class, "new"))
			.anySatisfy((entry) -> assertProperty(entry.newProperty(), "test.add.deprecated", String.class, "test2"));
		List<Difference> deleted = differences.differences()
			.stream()
			.filter((difference) -> difference.type() == DifferenceType.DELETED)
			.toList();
		assertThat(deleted).hasSize(4)
			.anySatisfy((entry) -> assertProperty(entry.oldProperty(), "test.replace", String.class, "replace"))
			.anySatisfy((entry) -> assertProperty(entry.oldProperty(), "test.delete", String.class, "delete"))
			.anySatisfy(
					(entry) -> assertProperty(entry.newProperty(), "test.delete.deprecated", String.class, "delete"))
			.anySatisfy((entry) -> assertProperty(entry.newProperty(), "test.removed.directly", String.class,
					"directlyRemoved"));
		List<Difference> deprecated = differences.differences()
			.stream()
			.filter((difference) -> difference.type() == DifferenceType.DEPRECATED)
			.toList();
		assertThat(deprecated).hasSize(1);
		assertProperty(deprecated.get(0).oldProperty(), "test.deprecate", String.class, "wrong");
		assertProperty(deprecated.get(0).newProperty(), "test.deprecate", String.class, "wrong");
	}

	private void assertProperty(ConfigurationMetadataProperty property, String id, Class<?> type, Object defaultValue) {
		assertThat(property).isNotNull();
		assertThat(property.getId()).isEqualTo(id);
		assertThat(property.getType()).isEqualTo(type.getName());
		assertThat(property.getDefaultValue()).isEqualTo(defaultValue);
	}

}
