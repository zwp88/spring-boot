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

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.r2dbc.init.R2dbcScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link R2dbcInitializationAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class R2dbcInitializationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(R2dbcInitializationAutoConfiguration.class))
		.withPropertyValues("spring.r2dbc.generate-unique-name:true");

	@Test
	void whenNoConnectionFactoryIsAvailableThenAutoConfigurationBacksOff() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenConnectionFactoryIsAvailableThenR2dbcInitializerIsAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(R2dbcScriptDatabaseInitializer.class));
	}

	@Test
	void whenConnectionFactoryIsAvailableAndModeIsNeverThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withPropertyValues("spring.sql.init.mode:never")
			.run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
	}

	@Test
	void whenAnSqlInitializerIsDefinedThenInitializerIsNotAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withUserConfiguration(SqlDatabaseInitializerConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(AbstractScriptDatabaseInitializer.class)
				.hasBean("customInitializer"));
	}

	@Test
	void whenAnInitializerIsDefinedThenSqlInitializerIsStillAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withUserConfiguration(DatabaseInitializerConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ApplicationScriptDatabaseInitializer.class)
				.hasBean("customInitializer"));
	}

	@Test
	void whenBeanIsAnnotatedAsDependingOnDatabaseInitializationThenItDependsOnR2dbcScriptDatabaseInitializer() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withUserConfiguration(DependsOnInitializedDatabaseConfiguration.class)
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(
						"r2dbcInitializationAutoConfigurationTests.DependsOnInitializedDatabaseConfiguration");
				assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("r2dbcScriptDatabaseInitializer");
			});
	}

	@Test
	void whenBeanIsAnnotatedAsDependingOnDatabaseInitializationThenItDependsOnDataSourceScriptDatabaseInitializer() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withUserConfiguration(DependsOnInitializedDatabaseConfiguration.class)
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(
						"r2dbcInitializationAutoConfigurationTests.DependsOnInitializedDatabaseConfiguration");
				assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("r2dbcScriptDatabaseInitializer");
			});
	}

	@Test
	void whenAConnectionFactoryIsAvailableAndSpringR2dbcIsNotThenAutoConfigurationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(DatabasePopulator.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class);
				assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class SqlDatabaseInitializerConfiguration {

		@Bean
		ApplicationScriptDatabaseInitializer customInitializer() {
			return mock(ApplicationScriptDatabaseInitializer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DatabaseInitializerConfiguration {

		@Bean
		R2dbcScriptDatabaseInitializer customInitializer() {
			return new R2dbcScriptDatabaseInitializer(mock(ConnectionFactory.class),
					new DatabaseInitializationSettings()) {

				@Override
				protected void runScripts(Scripts scripts) {
					// No-op
				}

				@Override
				protected boolean isEmbeddedDatabase() {
					return true;
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@DependsOnDatabaseInitialization
	static class DependsOnInitializedDatabaseConfiguration {

		DependsOnInitializedDatabaseConfiguration() {

		}

	}

}
