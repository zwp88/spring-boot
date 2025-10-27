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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.util.Assert;

/**
 * Details required to establish a connection to an SQL service using JDBC.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface JdbcConnectionDetails extends ConnectionDetails {

	/**
	 * Username for the database.
	 * @return the username for the database
	 */
	@Nullable String getUsername();

	/**
	 * Password for the database.
	 * @return the password for the database
	 */
	@Nullable String getPassword();

	/**
	 * JDBC url for the database.
	 * @return the JDBC url for the database
	 */
	String getJdbcUrl();

	/**
	 * The name of the JDBC driver class. Defaults to the class name of the driver
	 * specified in the JDBC URL.
	 * @return the JDBC driver class name
	 * @see #getJdbcUrl()
	 * @see DatabaseDriver#fromJdbcUrl(String)
	 * @see DatabaseDriver#getDriverClassName()
	 */
	default String getDriverClassName() {
		String driverClassName = DatabaseDriver.fromJdbcUrl(getJdbcUrl()).getDriverClassName();
		Assert.state(driverClassName != null, "'driverClassName' must not be null");
		return driverClassName;
	}

	/**
	 * Returns the name of the XA DataSource class. Defaults to the class name from the
	 * driver specified in the JDBC URL.
	 * @return the XA DataSource class name
	 * @see #getJdbcUrl()
	 * @see DatabaseDriver#fromJdbcUrl(String)
	 * @see DatabaseDriver#getXaDataSourceClassName()
	 */
	default @Nullable String getXaDataSourceClassName() {
		return DatabaseDriver.fromJdbcUrl(getJdbcUrl()).getXaDataSourceClassName();
	}

}
