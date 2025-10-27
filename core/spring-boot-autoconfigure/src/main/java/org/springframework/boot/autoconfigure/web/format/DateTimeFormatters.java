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

package org.springframework.boot.autoconfigure.web.format;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * {@link DateTimeFormatter Formatters} for dates, times, and date-times.
 *
 * @author Andy Wilkinson
 * @author Gaurav Pareek
 * @since 2.3.0
 */
public class DateTimeFormatters {

	private @Nullable DateTimeFormatter dateFormatter;

	private @Nullable String datePattern;

	private @Nullable DateTimeFormatter timeFormatter;

	private @Nullable DateTimeFormatter dateTimeFormatter;

	/**
	 * Configures the date format using the given {@code pattern}.
	 * @param pattern the pattern for formatting dates
	 * @return {@code this} for chained method invocation
	 */
	public DateTimeFormatters dateFormat(@Nullable String pattern) {
		if (isIso(pattern)) {
			this.dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
			this.datePattern = "yyyy-MM-dd";
		}
		else {
			this.dateFormatter = formatter(pattern);
			this.datePattern = pattern;
		}
		return this;
	}

	/**
	 * Configures the time format using the given {@code pattern}.
	 * @param pattern the pattern for formatting times
	 * @return {@code this} for chained method invocation
	 */
	public DateTimeFormatters timeFormat(@Nullable String pattern) {
		this.timeFormatter = isIso(pattern) ? DateTimeFormatter.ISO_LOCAL_TIME
				: (isIsoOffset(pattern) ? DateTimeFormatter.ISO_OFFSET_TIME : formatter(pattern));
		return this;
	}

	/**
	 * Configures the date-time format using the given {@code pattern}.
	 * @param pattern the pattern for formatting date-times
	 * @return {@code this} for chained method invocation
	 */
	public DateTimeFormatters dateTimeFormat(@Nullable String pattern) {
		this.dateTimeFormatter = isIso(pattern) ? DateTimeFormatter.ISO_LOCAL_DATE_TIME
				: (isIsoOffset(pattern) ? DateTimeFormatter.ISO_OFFSET_DATE_TIME : formatter(pattern));
		return this;
	}

	@Nullable DateTimeFormatter getDateFormatter() {
		return this.dateFormatter;
	}

	@Nullable String getDatePattern() {
		return this.datePattern;
	}

	@Nullable DateTimeFormatter getTimeFormatter() {
		return this.timeFormatter;
	}

	@Nullable DateTimeFormatter getDateTimeFormatter() {
		return this.dateTimeFormatter;
	}

	boolean isCustomized() {
		return this.dateFormatter != null || this.timeFormatter != null || this.dateTimeFormatter != null;
	}

	private static @Nullable DateTimeFormatter formatter(@Nullable String pattern) {
		return StringUtils.hasText(pattern)
				? DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.SMART) : null;
	}

	private static boolean isIso(@Nullable String pattern) {
		return "iso".equalsIgnoreCase(pattern);
	}

	private static boolean isIsoOffset(@Nullable String pattern) {
		return "isooffset".equalsIgnoreCase(pattern) || "iso-offset".equalsIgnoreCase(pattern);
	}

}
