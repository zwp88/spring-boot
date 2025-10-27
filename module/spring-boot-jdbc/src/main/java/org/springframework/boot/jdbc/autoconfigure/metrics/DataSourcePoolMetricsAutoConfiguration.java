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

package org.springframework.boot.jdbc.autoconfigure.metrics;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metrics.DataSourcePoolMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link DataSource datasources}.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 4.0.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ DataSource.class, MeterRegistry.class })
@ConditionalOnBean({ DataSource.class, MeterRegistry.class })
public final class DataSourcePoolMetricsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(DataSourcePoolMetadataProvider.class)
	static class DataSourcePoolMetadataMetricsConfiguration {

		private static final String DATASOURCE_SUFFIX = "dataSource";

		@Bean
		DataSourcePoolMetadataMeterBinder dataSourcePoolMetadataMeterBinder(ConfigurableListableBeanFactory beanFactory,
				ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders) {
			return new DataSourcePoolMetadataMeterBinder(SimpleAutowireCandidateResolver
				.resolveAutowireCandidates(beanFactory, DataSource.class, false, true), metadataProviders);
		}

		static class DataSourcePoolMetadataMeterBinder implements MeterBinder {

			private final Map<String, DataSource> dataSources;

			private final ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders;

			DataSourcePoolMetadataMeterBinder(Map<String, DataSource> dataSources,
					ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders) {
				this.dataSources = dataSources;
				this.metadataProviders = metadataProviders;
			}

			@Override
			public void bindTo(MeterRegistry registry) {
				List<DataSourcePoolMetadataProvider> metadataProvidersList = this.metadataProviders.stream().toList();
				this.dataSources.forEach((name, dataSource) -> bindDataSourceToRegistry(name, dataSource,
						metadataProvidersList, registry));
			}

			private void bindDataSourceToRegistry(String beanName, DataSource dataSource,
					Collection<DataSourcePoolMetadataProvider> metadataProviders, MeterRegistry registry) {
				String dataSourceName = getDataSourceName(beanName);
				new DataSourcePoolMetrics(dataSource, metadataProviders, dataSourceName, Collections.emptyList())
					.bindTo(registry);
			}

			/**
			 * Get the name of a DataSource based on its {@code beanName}.
			 * @param beanName the name of the data source bean
			 * @return a name for the given data source
			 */
			private String getDataSourceName(String beanName) {
				if (beanName.length() > DATASOURCE_SUFFIX.length()
						&& StringUtils.endsWithIgnoreCase(beanName, DATASOURCE_SUFFIX)) {
					return beanName.substring(0, beanName.length() - DATASOURCE_SUFFIX.length());
				}
				return beanName;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	static class HikariDataSourceMetricsConfiguration {

		@Bean
		HikariDataSourceMeterBinder hikariDataSourceMeterBinder(ObjectProvider<DataSource> dataSources) {
			return new HikariDataSourceMeterBinder(dataSources);
		}

		static class HikariDataSourceMeterBinder implements MeterBinder {

			private static final Log logger = LogFactory.getLog(HikariDataSourceMeterBinder.class);

			private final ObjectProvider<DataSource> dataSources;

			HikariDataSourceMeterBinder(ObjectProvider<DataSource> dataSources) {
				this.dataSources = dataSources;
			}

			@Override
			public void bindTo(MeterRegistry registry) {
				this.dataSources.stream(ObjectProvider.UNFILTERED, false).forEach((dataSource) -> {
					HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class,
							HikariDataSource.class);
					if (hikariDataSource != null) {
						bindMetricsRegistryToHikariDataSource(hikariDataSource, registry);
					}
				});
			}

			private void bindMetricsRegistryToHikariDataSource(HikariDataSource hikari, MeterRegistry registry) {
				if (hikari.getMetricRegistry() == null && hikari.getMetricsTrackerFactory() == null) {
					try {
						hikari.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));
					}
					catch (Exception ex) {
						logger.warn(LogMessage.format("Failed to bind Hikari metrics: %s", ex.getMessage()));
					}
				}
			}

		}

	}

}
