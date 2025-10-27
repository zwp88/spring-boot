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

package org.springframework.boot.micrometer.metrics.autoconfigure;

import java.time.Duration;

import io.micrometer.core.instrument.Meter.Type;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServiceLevelObjectiveBoundary}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ServiceLevelObjectiveBoundaryTests {

	@Test
	void getValueForTimerWhenFromLongShouldReturnMsToNanosValue() {
		ServiceLevelObjectiveBoundary slo = ServiceLevelObjectiveBoundary.valueOf(123L);
		assertThat(slo.getValue(Type.TIMER)).isEqualTo(123000000);
	}

	@Test
	void getValueForTimerWhenFromNumberStringShouldMsToNanosValue() {
		ServiceLevelObjectiveBoundary slo = ServiceLevelObjectiveBoundary.valueOf("123");
		assertThat(slo.getValue(Type.TIMER)).isEqualTo(123000000);
	}

	@Test
	void getValueForTimerWhenFromMillisecondDurationStringShouldReturnDurationNanos() {
		ServiceLevelObjectiveBoundary slo = ServiceLevelObjectiveBoundary.valueOf("123ms");
		assertThat(slo.getValue(Type.TIMER)).isEqualTo(123000000);
	}

	@Test
	void getValueForTimerWhenFromDaysDurationStringShouldReturnDurationNanos() {
		ServiceLevelObjectiveBoundary slo = ServiceLevelObjectiveBoundary.valueOf("1d");
		assertThat(slo.getValue(Type.TIMER)).isEqualTo(Duration.ofDays(1).toNanos());
	}

	@Test
	void getValueForDistributionSummaryWhenFromDoubleShouldReturnDoubleValue() {
		ServiceLevelObjectiveBoundary slo = ServiceLevelObjectiveBoundary.valueOf(123.42);
		assertThat(slo.getValue(Type.DISTRIBUTION_SUMMARY)).isEqualTo(123.42);
	}

	@Test
	void getValueForDistributionSummaryWhenFromStringShouldReturnDoubleValue() {
		ServiceLevelObjectiveBoundary slo = ServiceLevelObjectiveBoundary.valueOf("123.42");
		assertThat(slo.getValue(Type.DISTRIBUTION_SUMMARY)).isEqualTo(123.42);
	}

	@Test
	void getValueForDistributionSummaryWhenFromDurationShouldReturnNull() {
		ServiceLevelObjectiveBoundary slo = ServiceLevelObjectiveBoundary.valueOf("123ms");
		assertThat(slo.getValue(Type.DISTRIBUTION_SUMMARY)).isNull();
	}

	@Test
	void shouldRegisterRuntimeHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new ServiceLevelObjectiveBoundary.ServiceLevelObjectiveBoundaryHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		ReflectionUtils.doWithLocalMethods(ServiceLevelObjectiveBoundary.class, (method) -> {
			if ("valueOf".equals(method.getName())) {
				assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(method)).accepts(runtimeHints);
			}
		});
	}

}
