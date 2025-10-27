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

package org.springframework.boot.gson.autoconfigure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link GsonBuilder} to fine-tune its auto-configuration before it creates a
 * {@link Gson} instance.
 *
 * @author Ivan Golovko
 * @since 4.0.0
 */
@FunctionalInterface
public interface GsonBuilderCustomizer {

	/**
	 * Customize the GsonBuilder.
	 * @param gsonBuilder the GsonBuilder to customize
	 */
	void customize(GsonBuilder gsonBuilder);

}
