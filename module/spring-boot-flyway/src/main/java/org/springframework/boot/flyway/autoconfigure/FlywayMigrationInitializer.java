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

package org.springframework.boot.flyway.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * {@link InitializingBean} used to trigger {@link Flyway} migration through the
 * {@link FlywayMigrationStrategy}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class FlywayMigrationInitializer implements InitializingBean, Ordered {

	private final Flyway flyway;

	private final @Nullable FlywayMigrationStrategy migrationStrategy;

	private int order = 0;

	/**
	 * Create a new {@link FlywayMigrationInitializer} instance.
	 * @param flyway the flyway instance
	 */
	public FlywayMigrationInitializer(Flyway flyway) {
		this(flyway, null);
	}

	/**
	 * Create a new {@link FlywayMigrationInitializer} instance.
	 * @param flyway the flyway instance
	 * @param migrationStrategy the migration strategy or {@code null}
	 */
	public FlywayMigrationInitializer(Flyway flyway, @Nullable FlywayMigrationStrategy migrationStrategy) {
		Assert.notNull(flyway, "'flyway' must not be null");
		this.flyway = flyway;
		this.migrationStrategy = migrationStrategy;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.migrationStrategy != null) {
			this.migrationStrategy.migrate(this.flyway);
		}
		else {
			try {
				this.flyway.migrate();
			}
			catch (NoSuchMethodError ex) {
				// Flyway < 7.0
				this.flyway.getClass().getMethod("migrate").invoke(this.flyway);
			}
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
