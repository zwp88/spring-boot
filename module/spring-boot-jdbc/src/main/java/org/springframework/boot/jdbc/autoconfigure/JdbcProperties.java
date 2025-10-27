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

package org.springframework.boot.jdbc.autoconfigure;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * Configuration properties for JDBC.
 *
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.jdbc")
public class JdbcProperties {

	private final Template template = new Template();

	public Template getTemplate() {
		return this.template;
	}

	/**
	 * {@code JdbcTemplate} settings.
	 */
	public static class Template {

		/**
		 * Whether to ignore JDBC statement warnings (SQLWarning). When set to false,
		 * throw an SQLWarningException instead.
		 */
		private boolean ignoreWarnings = true;

		/**
		 * Number of rows that should be fetched from the database when more rows are
		 * needed. Use -1 to use the JDBC driver's default configuration.
		 */
		private int fetchSize = -1;

		/**
		 * Maximum number of rows. Use -1 to use the JDBC driver's default configuration.
		 */
		private int maxRows = -1;

		/**
		 * Query timeout. Default is to use the JDBC driver's default configuration. If a
		 * duration suffix is not specified, seconds will be used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration queryTimeout;

		/**
		 * Whether results processing should be skipped. Can be used to optimize callable
		 * statement processing when we know that no results are being passed back.
		 */
		private boolean skipResultsProcessing;

		/**
		 * Whether undeclared results should be skipped.
		 */
		private boolean skipUndeclaredResults;

		/**
		 * Whether execution of a CallableStatement will return the results in a Map that
		 * uses case-insensitive names for the parameters.
		 */
		private boolean resultsMapCaseInsensitive;

		public boolean isIgnoreWarnings() {
			return this.ignoreWarnings;
		}

		public void setIgnoreWarnings(boolean ignoreWarnings) {
			this.ignoreWarnings = ignoreWarnings;
		}

		public int getFetchSize() {
			return this.fetchSize;
		}

		public void setFetchSize(int fetchSize) {
			this.fetchSize = fetchSize;
		}

		public int getMaxRows() {
			return this.maxRows;
		}

		public void setMaxRows(int maxRows) {
			this.maxRows = maxRows;
		}

		public @Nullable Duration getQueryTimeout() {
			return this.queryTimeout;
		}

		public void setQueryTimeout(@Nullable Duration queryTimeout) {
			this.queryTimeout = queryTimeout;
		}

		public boolean isSkipResultsProcessing() {
			return this.skipResultsProcessing;
		}

		public void setSkipResultsProcessing(boolean skipResultsProcessing) {
			this.skipResultsProcessing = skipResultsProcessing;
		}

		public boolean isSkipUndeclaredResults() {
			return this.skipUndeclaredResults;
		}

		public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
			this.skipUndeclaredResults = skipUndeclaredResults;
		}

		public boolean isResultsMapCaseInsensitive() {
			return this.resultsMapCaseInsensitive;
		}

		public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
			this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
		}

	}

}
