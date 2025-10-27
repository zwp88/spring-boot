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

package org.springframework.boot.hazelcast.autoconfigure;

import com.hazelcast.core.HazelcastInstance;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastJpaDependencyAutoConfiguration.HazelcastInstanceEntityManagerFactoryDependsOnConfiguration;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Additional configuration to ensure that {@link EntityManagerFactory} beans depend on
 * the {@code hazelcastInstance} bean.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = HazelcastAutoConfiguration.class,
		afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@ConditionalOnClass({ HazelcastInstance.class, LocalContainerEntityManagerFactoryBean.class })
@Import(HazelcastInstanceEntityManagerFactoryDependsOnConfiguration.class)
public final class HazelcastJpaDependencyAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(EntityManagerFactoryDependsOnPostProcessor.class)
	@Conditional(OnHazelcastAndJpaCondition.class)
	@Import(HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor.class)
	static class HazelcastInstanceEntityManagerFactoryDependsOnConfiguration {

	}

	static class HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor() {
			super("hazelcastInstance");
		}

	}

	static class OnHazelcastAndJpaCondition extends AllNestedConditions {

		OnHazelcastAndJpaCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(name = "hazelcastInstance")
		static class HasHazelcastInstance {

		}

		@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
		static class HasJpa {

		}

	}

}
