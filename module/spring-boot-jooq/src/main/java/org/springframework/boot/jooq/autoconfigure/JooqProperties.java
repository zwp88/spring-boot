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

package org.springframework.boot.jooq.autoconfigure;

import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for the jOOQ database library.
 *
 * @author Andreas Ahlenstorf
 * @author Michael Simons
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("spring.jooq")
public class JooqProperties {

	/**
	 * SQL dialect to use. Auto-detected by default.
	 */
	private @Nullable SQLDialect sqlDialect;

	/**
	 * Location of the jOOQ config file.
	 */
	private @Nullable Resource config;

	public @Nullable SQLDialect getSqlDialect() {
		return this.sqlDialect;
	}

	public void setSqlDialect(@Nullable SQLDialect sqlDialect) {
		this.sqlDialect = sqlDialect;
	}

	public @Nullable Resource getConfig() {
		return this.config;
	}

	public void setConfig(@Nullable Resource config) {
		this.config = config;
	}

	/**
	 * Determine the {@link SQLDialect} to use based on this configuration and the primary
	 * {@link DataSource}.
	 * @param dataSource the data source
	 * @return the {@code SQLDialect} to use for that {@link DataSource}
	 */
	public SQLDialect determineSqlDialect(DataSource dataSource) {
		if (this.sqlDialect != null) {
			return this.sqlDialect;
		}
		return SqlDialectLookup.getDialect(dataSource);
	}

}
