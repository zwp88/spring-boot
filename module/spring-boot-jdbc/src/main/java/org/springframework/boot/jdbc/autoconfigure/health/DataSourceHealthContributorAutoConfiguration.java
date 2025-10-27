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

package org.springframework.boot.jdbc.autoconfigure.health;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator;
import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataSourceHealthIndicator}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Arthur Kalimullin
 * @author Julio Gomez
 * @author Safeer Ansari
 * @since 4.0.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({ JdbcTemplate.class, AbstractRoutingDataSource.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(DataSource.class)
@ConditionalOnEnabledHealthIndicator("db")
@EnableConfigurationProperties(DataSourceHealthIndicatorProperties.class)
public final class DataSourceHealthContributorAutoConfiguration implements InitializingBean {

	private final Collection<DataSourcePoolMetadataProvider> metadataProviders;

	@SuppressWarnings("NullAway.Init")
	private DataSourcePoolMetadataProvider poolMetadataProvider;

	DataSourceHealthContributorAutoConfiguration(ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders) {
		this.metadataProviders = metadataProviders.orderedStream().toList();
	}

	@Override
	public void afterPropertiesSet() {
		this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(this.metadataProviders);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "dbHealthIndicator", "dbHealthContributor" })
	HealthContributor dbHealthContributor(ConfigurableListableBeanFactory beanFactory,
			DataSourceHealthIndicatorProperties dataSourceHealthIndicatorProperties) {
		Map<String, DataSource> dataSources = SimpleAutowireCandidateResolver.resolveAutowireCandidates(beanFactory,
				DataSource.class, false, true);
		if (dataSourceHealthIndicatorProperties.isIgnoreRoutingDataSources()) {
			Map<String, DataSource> filteredDatasources = dataSources.entrySet()
				.stream()
				.filter((e) -> !isRoutingDataSource(e.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return createContributor(filteredDatasources);
		}
		return createContributor(dataSources);
	}

	private HealthContributor createContributor(Map<String, DataSource> beans) {
		Assert.notEmpty(beans, "'beans' must not be empty");
		if (beans.size() == 1) {
			return createContributor(beans.values().iterator().next());
		}
		return CompositeHealthContributor.fromMap(beans, this::createContributor);
	}

	private HealthContributor createContributor(DataSource source) {
		if (isRoutingDataSource(source)) {
			return new RoutingDataSourceHealthContributor(extractRoutingDataSource(source), this::createContributor);
		}
		return new DataSourceHealthIndicator(source, getValidationQuery(source));
	}

	private @Nullable String getValidationQuery(DataSource source) {
		DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider.getDataSourcePoolMetadata(source);
		return (poolMetadata != null) ? poolMetadata.getValidationQuery() : null;
	}

	private static boolean isRoutingDataSource(DataSource dataSource) {
		if (dataSource instanceof AbstractRoutingDataSource) {
			return true;
		}
		try {
			return dataSource.isWrapperFor(AbstractRoutingDataSource.class);
		}
		catch (SQLException ex) {
			return false;
		}
	}

	private static AbstractRoutingDataSource extractRoutingDataSource(DataSource dataSource) {
		if (dataSource instanceof AbstractRoutingDataSource routingDataSource) {
			return routingDataSource;
		}
		try {
			return dataSource.unwrap(AbstractRoutingDataSource.class);
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Failed to unwrap AbstractRoutingDataSource from " + dataSource, ex);
		}
	}

	/**
	 * {@link CompositeHealthContributor} used for {@link AbstractRoutingDataSource} beans
	 * where the overall health is composed of a {@link DataSourceHealthIndicator} for
	 * each routed datasource.
	 */
	static class RoutingDataSourceHealthContributor implements CompositeHealthContributor {

		private final CompositeHealthContributor delegate;

		private static final String UNNAMED_DATASOURCE_KEY = "unnamed";

		RoutingDataSourceHealthContributor(AbstractRoutingDataSource routingDataSource,
				Function<DataSource, HealthContributor> contributorFunction) {
			Map<String, DataSource> routedDataSources = routingDataSource.getResolvedDataSources()
				.entrySet()
				.stream()
				.collect(Collectors.toMap((e) -> Objects.toString(e.getKey(), UNNAMED_DATASOURCE_KEY),
						Map.Entry::getValue));
			this.delegate = CompositeHealthContributor.fromMap(routedDataSources, contributorFunction);
		}

		@Override
		public @Nullable HealthContributor getContributor(String name) {
			return this.delegate.getContributor(name);
		}

		@Override
		public Stream<HealthContributors.Entry> stream() {
			return this.delegate.stream();
		}

	}

}
