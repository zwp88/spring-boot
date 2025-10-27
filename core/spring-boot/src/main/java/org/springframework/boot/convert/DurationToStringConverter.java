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

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * {@link Converter} to convert from a {@link Duration} to a {@link String}.
 *
 * @author Phillip Webb
 * @see DurationFormat
 * @see DurationUnit
 */
final class DurationToStringConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Duration.class, String.class));
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return convert((Duration) source, getDurationStyle(sourceType), getDurationUnit(sourceType));
	}

	private @Nullable ChronoUnit getDurationUnit(TypeDescriptor sourceType) {
		DurationUnit annotation = sourceType.getAnnotation(DurationUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private @Nullable DurationStyle getDurationStyle(TypeDescriptor sourceType) {
		DurationFormat annotation = sourceType.getAnnotation(DurationFormat.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private String convert(Duration source, @Nullable DurationStyle style, @Nullable ChronoUnit unit) {
		style = (style != null) ? style : DurationStyle.ISO8601;
		return style.print(source, unit);
	}

}
