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

package org.springframework.boot.hibernate.autoconfigure.metrics;

import java.util.Collections;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.HibernateMetrics;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * Hibernate {@link EntityManagerFactory} instances that have statistics enabled.
 *
 * @author Rui Figueira
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ EntityManagerFactory.class, SessionFactory.class, HibernateMetrics.class, MeterRegistry.class })
@ConditionalOnBean({ EntityManagerFactory.class, MeterRegistry.class })
public final class HibernateMetricsAutoConfiguration implements SmartInitializingSingleton {

	private static final String ENTITY_MANAGER_FACTORY_SUFFIX = "entityManagerFactory";

	private final Map<String, EntityManagerFactory> entityManagerFactories;

	private final MeterRegistry meterRegistry;

	HibernateMetricsAutoConfiguration(ConfigurableListableBeanFactory beanFactory, MeterRegistry meterRegistry) {
		this.entityManagerFactories = SimpleAutowireCandidateResolver.resolveAutowireCandidates(beanFactory,
				EntityManagerFactory.class);
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void afterSingletonsInstantiated() {
		bindEntityManagerFactoriesToRegistry(this.entityManagerFactories, this.meterRegistry);
	}

	private void bindEntityManagerFactoriesToRegistry(Map<String, EntityManagerFactory> entityManagerFactories,
			MeterRegistry registry) {
		entityManagerFactories.forEach((name, factory) -> bindEntityManagerFactoryToRegistry(name, factory, registry));
	}

	private void bindEntityManagerFactoryToRegistry(String beanName, EntityManagerFactory entityManagerFactory,
			MeterRegistry registry) {
		String entityManagerFactoryName = getEntityManagerFactoryName(beanName);
		try {
			new HibernateMetrics(entityManagerFactory.unwrap(SessionFactory.class), entityManagerFactoryName,
					Collections.emptyList())
				.bindTo(registry);
		}
		catch (PersistenceException ex) {
			// Continue
		}
	}

	/**
	 * Get the name of an {@link EntityManagerFactory} based on its {@code beanName}.
	 * @param beanName the name of the {@link EntityManagerFactory} bean
	 * @return a name for the given entity manager factory
	 */
	private String getEntityManagerFactoryName(String beanName) {
		if (beanName.length() > ENTITY_MANAGER_FACTORY_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, ENTITY_MANAGER_FACTORY_SUFFIX)) {
			return beanName.substring(0, beanName.length() - ENTITY_MANAGER_FACTORY_SUFFIX.length());
		}
		return beanName;
	}

}
