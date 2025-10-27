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
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayAutoConfiguration} with Flyway 10.0.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "flyway-core-*.jar", "flyway-sqlserver-*.jar" })
@ClassPathOverrides({ "org.flywaydb:flyway-core:10.0.0", "com.h2database:h2:2.1.210" })
class Flyway100AutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void defaultFlyway() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getConfiguration().getLocations())
				.containsExactly(createLocation("classpath:db/migration"));
		});
	}

	@SuppressWarnings("deprecation")
	private static Location createLocation(String location) {
		return new Location(location);
	}

}
