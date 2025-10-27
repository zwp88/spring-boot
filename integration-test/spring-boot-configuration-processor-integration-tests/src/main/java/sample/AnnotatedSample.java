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

package sample;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Test that a valid type is generated if an annotation is present.
 *
 * @author Stephane Nicoll
 * @since 1.5.10
 */
@ConfigurationProperties("annotated")
public class AnnotatedSample {

	/**
	 * A valid name.
	 */
	private @Nullable String name;

	@Valid
	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

}
