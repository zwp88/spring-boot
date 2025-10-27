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

package org.springframework.boot.hibernate.autoconfigure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Hibernate.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 4.0.0
 * @see JpaProperties
 */
@ConfigurationProperties("spring.jpa.hibernate")
public class HibernateProperties {

	private static final String DISABLED_SCANNER_CLASS = "org.hibernate.boot.archive.scan.internal.DisabledScanner";

	private final Naming naming = new Naming();

	/**
	 * DDL mode. This is actually a shortcut for the "hibernate.hbm2ddl.auto" property.
	 * Defaults to "create-drop" when using an embedded database and no schema manager was
	 * detected. Otherwise, defaults to "none".
	 */
	private @Nullable String ddlAuto;

	public @Nullable String getDdlAuto() {
		return this.ddlAuto;
	}

	public void setDdlAuto(@Nullable String ddlAuto) {
		this.ddlAuto = ddlAuto;
	}

	public Naming getNaming() {
		return this.naming;
	}

	/**
	 * Determine the configuration properties for the initialization of the main Hibernate
	 * EntityManagerFactory based on standard JPA properties and
	 * {@link HibernateSettings}.
	 * @param jpaProperties standard JPA properties
	 * @param settings the settings to apply when determining the configuration properties
	 * @return the Hibernate properties to use
	 */
	public Map<String, Object> determineHibernateProperties(Map<String, String> jpaProperties,
			HibernateSettings settings) {
		Assert.notNull(jpaProperties, "'jpaProperties' must not be null");
		Assert.notNull(settings, "'settings' must not be null");
		return getAdditionalProperties(jpaProperties, settings);
	}

	private Map<String, Object> getAdditionalProperties(Map<String, String> existing, HibernateSettings settings) {
		Map<String, Object> result = new HashMap<>(existing);
		applyScanner(result);
		getNaming().applyNamingStrategies(result);
		String ddlAuto = determineDdlAuto(existing, settings::getDdlAuto);
		if (StringUtils.hasText(ddlAuto) && !"none".equals(ddlAuto)) {
			result.put(SchemaToolingSettings.HBM2DDL_AUTO, ddlAuto);
		}
		else {
			result.remove(SchemaToolingSettings.HBM2DDL_AUTO);
		}
		Collection<HibernatePropertiesCustomizer> customizers = settings.getHibernatePropertiesCustomizers();
		if (!ObjectUtils.isEmpty(customizers)) {
			customizers.forEach((customizer) -> customizer.customize(result));
		}
		return result;
	}

	private void applyScanner(Map<String, Object> result) {
		if (!result.containsKey(PersistenceSettings.SCANNER) && ClassUtils.isPresent(DISABLED_SCANNER_CLASS, null)) {
			result.put(PersistenceSettings.SCANNER, DISABLED_SCANNER_CLASS);
		}
	}

	private @Nullable String determineDdlAuto(Map<String, String> existing, Supplier<@Nullable String> defaultDdlAuto) {
		String ddlAuto = existing.get(SchemaToolingSettings.HBM2DDL_AUTO);
		if (ddlAuto != null) {
			return ddlAuto;
		}
		if (this.ddlAuto != null) {
			return this.ddlAuto;
		}
		if (existing.get(SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION) != null) {
			return null;
		}
		return defaultDdlAuto.get();
	}

	public static class Naming {

		/**
		 * Fully qualified name of the implicit naming strategy.
		 */
		private @Nullable String implicitStrategy;

		/**
		 * Fully qualified name of the physical naming strategy.
		 */
		private @Nullable String physicalStrategy;

		public @Nullable String getImplicitStrategy() {
			return this.implicitStrategy;
		}

		public void setImplicitStrategy(@Nullable String implicitStrategy) {
			this.implicitStrategy = implicitStrategy;
		}

		public @Nullable String getPhysicalStrategy() {
			return this.physicalStrategy;
		}

		public void setPhysicalStrategy(@Nullable String physicalStrategy) {
			this.physicalStrategy = physicalStrategy;
		}

		private void applyNamingStrategies(Map<String, Object> properties) {
			applyNamingStrategy(properties, MappingSettings.IMPLICIT_NAMING_STRATEGY, this.implicitStrategy,
					SpringImplicitNamingStrategy.class::getName);
			applyNamingStrategy(properties, MappingSettings.PHYSICAL_NAMING_STRATEGY, this.physicalStrategy,
					PhysicalNamingStrategySnakeCaseImpl.class::getName);
		}

		private void applyNamingStrategy(Map<String, Object> properties, String key, @Nullable Object strategy,
				Supplier<String> defaultStrategy) {
			if (strategy != null) {
				properties.put(key, strategy);
			}
			else {
				properties.computeIfAbsent(key, (k) -> defaultStrategy.get());
			}
		}

	}

}
