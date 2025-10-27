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

package org.springframework.boot.micrometer.metrics.autoconfigure.task;

import java.util.Collection;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TaskExecutorMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class TaskExecutorMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class)
		.withConfiguration(
				AutoConfigurations.of(TaskExecutorMetricsAutoConfiguration.class, MetricsAutoConfiguration.class))
		.withPropertyValues("management.metrics.use-global-registry=false");

	@Test
	void taskExecutorUsingAutoConfigurationIsInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
				assertThat(meters).singleElement()
					.satisfies(
							(meter) -> assertThat(meter.getId().getTag("name")).isEqualTo("applicationTaskExecutor"));
				assertThatExceptionOfType(MeterNotFoundException.class)
					.isThrownBy(() -> registry.get("executor").timer());
			});
	}

	@Test
	void taskExecutorIsInstrumentedWhenUsingLazyInitialization() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.withBean(LazyInitializationBeanFactoryPostProcessor.class)
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
				assertThat(meters).singleElement()
					.satisfies(
							(meter) -> assertThat(meter.getId().getTag("name")).isEqualTo("applicationTaskExecutor"));
				assertThatExceptionOfType(MeterNotFoundException.class)
					.isThrownBy(() -> registry.get("executor").timer());
			});
	}

	@Test
	void taskExecutorsWithCustomNamesAreInstrumented() {
		this.contextRunner.withUserConfiguration(MultipleTaskExecutorsConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
			assertThat(meters).map((meter) -> meter.getId().getTag("name"))
				.containsExactlyInAnyOrder("standardTaskExecutor", "nonDefault");
		});
	}

	@Test
	void threadPoolTaskExecutorWithNoTaskExecutorIsIgnored() {
		ThreadPoolTaskExecutor unavailableTaskExecutor = mock(ThreadPoolTaskExecutor.class);
		given(unavailableTaskExecutor.getThreadPoolExecutor()).willThrow(new IllegalStateException("Test"));
		this.contextRunner.withBean("firstTaskExecutor", ThreadPoolTaskExecutor.class, ThreadPoolTaskExecutor::new)
			.withBean("customName", ThreadPoolTaskExecutor.class, () -> unavailableTaskExecutor)
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
				assertThat(meters).singleElement()
					.satisfies((meter) -> assertThat(meter.getId().getTag("name")).isEqualTo("firstTaskExecutor"));
			});
	}

	@Test
	void taskExecutorInstrumentationCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.executor=false")
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(
						registry.find("executor.completed").tags("name", "applicationTaskExecutor").functionCounter())
					.isNull();
			});
	}

	@Test
	void taskSchedulerUsingAutoConfigurationIsInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
			.withUserConfiguration(SchedulingTestConfiguration.class)
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
				assertThat(meters).singleElement()
					.satisfies((meter) -> assertThat(meter.getId().getTag("name")).isEqualTo("taskScheduler"));
				assertThatExceptionOfType(MeterNotFoundException.class)
					.isThrownBy(() -> registry.get("executor").timer());
			});
	}

	@Test
	void taskSchedulersWithCustomNamesAreInstrumented() {
		this.contextRunner.withUserConfiguration(MultipleTaskSchedulersConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
			assertThat(meters).map((meter) -> meter.getId().getTag("name"))
				.containsExactlyInAnyOrder("standardTaskScheduler", "nonDefault");
		});
	}

	@Test
	void threadPoolTaskSchedulerWithNoTaskExecutorIsIgnored() {
		ThreadPoolTaskScheduler unavailableTaskExecutor = mock(ThreadPoolTaskScheduler.class);
		given(unavailableTaskExecutor.getScheduledThreadPoolExecutor()).willThrow(new IllegalStateException("Test"));
		this.contextRunner.withBean("firstTaskScheduler", ThreadPoolTaskScheduler.class, ThreadPoolTaskScheduler::new)
			.withBean("customName", ThreadPoolTaskScheduler.class, () -> unavailableTaskExecutor)
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
				assertThat(meters).singleElement()
					.satisfies((meter) -> assertThat(meter.getId().getTag("name")).isEqualTo("firstTaskScheduler"));
			});
	}

	@Test
	void taskSchedulerInstrumentationCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.executor=false")
			.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
			.withUserConfiguration(SchedulingTestConfiguration.class)
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("executor.completed").tags("name", "taskScheduler").functionCounter())
					.isNull();
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	static class SchedulingTestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleTaskSchedulersConfiguration {

		@Bean
		ThreadPoolTaskScheduler standardTaskScheduler() {
			return new ThreadPoolTaskScheduler();
		}

		@Bean(defaultCandidate = false)
		ThreadPoolTaskScheduler nonDefault() {
			return new ThreadPoolTaskScheduler();
		}

		@Bean(autowireCandidate = false)
		ThreadPoolTaskScheduler nonAutowire() {
			return new ThreadPoolTaskScheduler();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleTaskExecutorsConfiguration {

		@Bean
		ThreadPoolTaskExecutor standardTaskExecutor() {
			return new ThreadPoolTaskExecutor();
		}

		@Bean(defaultCandidate = false)
		ThreadPoolTaskExecutor nonDefault() {
			return new ThreadPoolTaskExecutor();
		}

		@Bean(autowireCandidate = false)
		ThreadPoolTaskExecutor nonAutowire() {
			return new ThreadPoolTaskExecutor();
		}

	}

}
