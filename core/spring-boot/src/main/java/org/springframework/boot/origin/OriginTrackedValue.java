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

package org.springframework.boot.origin;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;
import org.springframework.util.ObjectUtils;

/**
 * A wrapper for an {@link Object} value and {@link Origin}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see #of(Object)
 * @see #of(Object, Origin)
 */
public class OriginTrackedValue implements OriginProvider {

	private final Object value;

	private final @Nullable Origin origin;

	private OriginTrackedValue(Object value, @Nullable Origin origin) {
		this.value = value;
		this.origin = origin;
	}

	/**
	 * Return the tracked value.
	 * @return the tracked value
	 */
	public Object getValue() {
		return this.value;
	}

	@Override
	public @Nullable Origin getOrigin() {
		return this.origin;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.value, ((OriginTrackedValue) obj).value);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public @Nullable String toString() {
		return (this.value != null) ? this.value.toString() : null;
	}

	@Contract("!null -> !null")
	public static @Nullable OriginTrackedValue of(@Nullable Object value) {
		return of(value, null);
	}

	/**
	 * Create an {@link OriginTrackedValue} containing the specified {@code value} and
	 * {@code origin}. If the source value implements {@link CharSequence} then so will
	 * the resulting {@link OriginTrackedValue}.
	 * @param value the source value
	 * @param origin the origin
	 * @return an {@link OriginTrackedValue} or {@code null} if the source value was
	 * {@code null}.
	 */
	@Contract("null, _ -> null; !null, _ -> !null")
	public static @Nullable OriginTrackedValue of(@Nullable Object value, @Nullable Origin origin) {
		if (value == null) {
			return null;
		}
		if (value instanceof CharSequence charSequence) {
			return new OriginTrackedCharSequence(charSequence, origin);
		}
		return new OriginTrackedValue(value, origin);
	}

	/**
	 * {@link OriginTrackedValue} for a {@link CharSequence}.
	 */
	private static class OriginTrackedCharSequence extends OriginTrackedValue implements CharSequence {

		OriginTrackedCharSequence(CharSequence value, @Nullable Origin origin) {
			super(value, origin);
		}

		@Override
		public int length() {
			return getValue().length();
		}

		@Override
		public char charAt(int index) {
			return getValue().charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return getValue().subSequence(start, end);
		}

		@Override
		public CharSequence getValue() {
			return (CharSequence) super.getValue();
		}

	}

}
