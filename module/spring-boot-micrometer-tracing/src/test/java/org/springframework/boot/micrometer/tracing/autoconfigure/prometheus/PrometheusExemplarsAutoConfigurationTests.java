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

package org.springframework.boot.micrometer.tracing.autoconfigure.prometheus;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.prometheus.metrics.expositionformats.OpenMetricsTextFormatWriter;
import io.prometheus.metrics.tracer.common.SpanContext;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.BraveAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrometheusExemplarsAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class PrometheusExemplarsAutoConfigurationTests {

	private static final Pattern BUCKET_TRACE_INFO_PATTERN = Pattern.compile(
			"^test_observation_seconds_bucket\\{error=\"none\",le=\".+\"} 1 # \\{span_id=\"(\\p{XDigit}+)\",trace_id=\"(\\p{XDigit}+)\"} .+$");

	private static final Pattern COUNT_TRACE_INFO_PATTERN = Pattern.compile(
			"^test_observation_seconds_count\\{error=\"none\"} 1 # \\{span_id=\"(\\p{XDigit}+)\",trace_id=\"(\\p{XDigit}+)\"} .+$");

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("management.tracing.sampling.probability=1.0",
				"management.metrics.distribution.percentiles-histogram.all=true",
				"management.metrics.use-global-registry=false")
		.withConfiguration(
				AutoConfigurations.of(MetricsAutoConfiguration.class, PrometheusMetricsExportAutoConfiguration.class,
						PrometheusExemplarsAutoConfiguration.class, ObservationAutoConfiguration.class,
						BraveAutoConfiguration.class, MicrometerTracingAutoConfiguration.class));

	@Test
	void shouldNotSupplyBeansIfPrometheusSupportIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.prometheus.metrics.tracer"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContext.class));
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerTracingIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.tracing"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContext.class));
	}

	@Test
	void shouldSupplyCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(SpanContext.class)
				.getBean(SpanContext.class)
				.isSameAs(CustomConfiguration.SPAN_CONTEXT));
	}

	@Test
	void prometheusOpenMetricsOutputWithoutExemplarsOnHistogramCount() {
		this.contextRunner.withUserConfiguration(TracingConfiguration.class)
			.withPropertyValues(
					"management.prometheus.metrics.export.properties.io.prometheus.exporter.exemplarsOnAllMetricTypes=false")
			.run((context) -> {
				assertThat(context).hasSingleBean(SpanContext.class);
				ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
				Observation.start("test.observation", observationRegistry).stop();
				PrometheusMeterRegistry prometheusMeterRegistry = context.getBean(PrometheusMeterRegistry.class);
				String openMetricsOutput = prometheusMeterRegistry.scrape(OpenMetricsTextFormatWriter.CONTENT_TYPE);

				assertThat(openMetricsOutput).contains("test_observation_seconds_bucket");
				assertThat(openMetricsOutput).containsOnlyOnce("test_observation_seconds_count");
				assertThat(StringUtils.countOccurrencesOf(openMetricsOutput, "span_id")).isEqualTo(1);
				assertThat(StringUtils.countOccurrencesOf(openMetricsOutput, "trace_id")).isEqualTo(1);

				Optional<TraceInfo> bucketTraceInfo = openMetricsOutput.lines()
					.filter((line) -> line.contains("test_observation_seconds_bucket") && line.contains("span_id"))
					.map(BUCKET_TRACE_INFO_PATTERN::matcher)
					.flatMap(Matcher::results)
					.map((matchResult) -> new TraceInfo(matchResult.group(2), matchResult.group(1)))
					.findFirst();

				assertThat(bucketTraceInfo).isNotEmpty();
			});
	}

	@Test
	void prometheusOpenMetricsOutputShouldContainExemplars() {
		this.contextRunner.withUserConfiguration(TracingConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(SpanContext.class);
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test.observation", observationRegistry).stop();
			PrometheusMeterRegistry prometheusMeterRegistry = context.getBean(PrometheusMeterRegistry.class);
			String openMetricsOutput = prometheusMeterRegistry.scrape(OpenMetricsTextFormatWriter.CONTENT_TYPE);

			assertThat(openMetricsOutput).contains("test_observation_seconds_bucket");
			assertThat(openMetricsOutput).containsOnlyOnce("test_observation_seconds_count");
			assertThat(StringUtils.countOccurrencesOf(openMetricsOutput, "span_id")).isEqualTo(2);
			assertThat(StringUtils.countOccurrencesOf(openMetricsOutput, "trace_id")).isEqualTo(2);

			Optional<TraceInfo> bucketTraceInfo = openMetricsOutput.lines()
				.filter((line) -> line.contains("test_observation_seconds_bucket") && line.contains("span_id"))
				.map(BUCKET_TRACE_INFO_PATTERN::matcher)
				.flatMap(Matcher::results)
				.map((matchResult) -> new TraceInfo(matchResult.group(2), matchResult.group(1)))
				.findFirst();

			Optional<TraceInfo> counterTraceInfo = openMetricsOutput.lines()
				.filter((line) -> line.contains("test_observation_seconds_count") && line.contains("span_id"))
				.map(COUNT_TRACE_INFO_PATTERN::matcher)
				.flatMap(Matcher::results)
				.map((matchResult) -> new TraceInfo(matchResult.group(2), matchResult.group(1)))
				.findFirst();

			assertThat(bucketTraceInfo).isNotEmpty().contains(counterTraceInfo.orElse(null));
		});
	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomConfiguration {

		static final SpanContext SPAN_CONTEXT = mock(SpanContext.class);

		@Bean
		SpanContext customSpanContext() {
			return SPAN_CONTEXT;
		}

	}

	private record TraceInfo(String traceId, String spanId) {
	}

	@Configuration(proxyBeanMethods = false)
	static class TracingConfiguration {

		@Bean
		TracingAwareMeterObservationHandler<Observation.Context> tracingAwareMeterObservationHandler(
				MeterRegistry meterRegistry, Tracer tracer) {
			DefaultMeterObservationHandler delegate = new DefaultMeterObservationHandler(meterRegistry);
			return new TracingAwareMeterObservationHandler<>(delegate, tracer);
		}

	}

}
