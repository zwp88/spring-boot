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

package org.springframework.boot.micrometer.tracing.autoconfigure;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.DefaultNewSpanParser;
import io.micrometer.tracing.annotation.ImperativeMethodInvocationProcessor;
import io.micrometer.tracing.annotation.MethodInvocationProcessor;
import io.micrometer.tracing.annotation.NewSpanParser;
import io.micrometer.tracing.annotation.SpanAspect;
import io.micrometer.tracing.annotation.SpanTagAnnotationHandler;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.aspectj.weaver.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationHandlerGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Micrometer Tracing API.
 *
 * @author Moritz Halbritter
 * @author Jonatan Ivanov
 * @since 4.0.0
 */
@AutoConfiguration(after = ObservationAutoConfiguration.class)
@ConditionalOnBean(Tracer.class)
public final class MicrometerTracingAutoConfiguration {

	/**
	 * {@code @Order} value of {@link #defaultTracingObservationHandler(Tracer)}.
	 */
	public static final int DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER = Ordered.LOWEST_PRECEDENCE - 1000;

	/**
	 * {@code @Order} value of
	 * {@link #propagatingReceiverTracingObservationHandler(Tracer, Propagator)}.
	 */
	public static final int RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER = 1000;

	/**
	 * {@code @Order} value of
	 * {@link #propagatingSenderTracingObservationHandler(Tracer, Propagator)}.
	 */
	public static final int SENDER_TRACING_OBSERVATION_HANDLER_ORDER = 2000;

	@Bean
	ObservationHandlerGroup tracingObservationHandlerGroup(Tracer tracer) {
		return ClassUtils.isPresent("io.micrometer.core.instrument.MeterRegistry", null)
				? new TracingAndMeterObservationHandlerGroup(tracer)
				: ObservationHandlerGroup.of(TracingObservationHandler.class);
	}

	@Bean
	@ConditionalOnMissingBean
	@Order(DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER)
	DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
		return new DefaultTracingObservationHandler(tracer);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(Propagator.class)
	@Order(SENDER_TRACING_OBSERVATION_HANDLER_ORDER)
	PropagatingSenderTracingObservationHandler<?> propagatingSenderTracingObservationHandler(Tracer tracer,
			Propagator propagator) {
		return new PropagatingSenderTracingObservationHandler<>(tracer, propagator);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(Propagator.class)
	@Order(RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER)
	PropagatingReceiverTracingObservationHandler<?> propagatingReceiverTracingObservationHandler(Tracer tracer,
			Propagator propagator) {
		return new PropagatingReceiverTracingObservationHandler<>(tracer, propagator);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Advice.class)
	@ConditionalOnBooleanProperty("management.observations.annotations.enabled")
	static class SpanAspectConfiguration {

		@Bean
		@ConditionalOnMissingBean(NewSpanParser.class)
		DefaultNewSpanParser newSpanParser() {
			return new DefaultNewSpanParser();
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(ValueExpressionResolver.class)
		SpanTagAnnotationHandler spanTagAnnotationHandler(BeanFactory beanFactory,
				ValueExpressionResolver valueExpressionResolver) {
			return new SpanTagAnnotationHandler(beanFactory::getBean, (ignored) -> valueExpressionResolver);
		}

		@Bean
		@ConditionalOnMissingBean(MethodInvocationProcessor.class)
		ImperativeMethodInvocationProcessor imperativeMethodInvocationProcessor(NewSpanParser newSpanParser,
				Tracer tracer, ObjectProvider<SpanTagAnnotationHandler> spanTagAnnotationHandler) {
			return new ImperativeMethodInvocationProcessor(newSpanParser, tracer,
					spanTagAnnotationHandler.getIfAvailable());
		}

		@Bean
		@ConditionalOnMissingBean
		SpanAspect spanAspect(MethodInvocationProcessor methodInvocationProcessor) {
			return new SpanAspect(methodInvocationProcessor);
		}

	}

}
