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

package org.springframework.boot.cassandra.autoconfigure.health;

import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.cassandra.autoconfigure.health.CassandraHealthContributorConfigurations.CassandraDriverConfiguration;
import org.springframework.boot.cassandra.health.CassandraDriverHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CassandraDriverHealthIndicator}.
 *
 * @author Julien Dubois
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(
		after = { CassandraReactiveHealthContributorAutoConfiguration.class, CassandraAutoConfiguration.class })
@ConditionalOnClass({ CqlSession.class, CassandraDriverHealthIndicator.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnEnabledHealthIndicator("cassandra")
@Import(CassandraDriverConfiguration.class)
public final class CassandraHealthContributorAutoConfiguration {

}
