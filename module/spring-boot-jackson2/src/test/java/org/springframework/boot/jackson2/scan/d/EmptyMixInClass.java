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

package org.springframework.boot.jackson2.scan.d;

import org.springframework.boot.jackson2.JsonMixin;
import org.springframework.boot.jackson2.types.Name;
import org.springframework.boot.jackson2.types.NameAndAge;

@JsonMixin(type = { Name.class, NameAndAge.class })
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public class EmptyMixInClass {

}
