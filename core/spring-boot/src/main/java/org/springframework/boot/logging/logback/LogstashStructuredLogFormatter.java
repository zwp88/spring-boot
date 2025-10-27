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

package org.springframework.boot.logging.logback;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.structured.CommonStructuredLogFormat;
import org.springframework.boot.logging.structured.ContextPairs;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Logback {@link StructuredLogFormatter} for {@link CommonStructuredLogFormat#LOGSTASH}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class LogstashStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

	private static final PairExtractor<KeyValuePair> keyValuePairExtractor = PairExtractor.of((pair) -> pair.key,
			(pair) -> pair.value);

	LogstashStructuredLogFormatter(@Nullable StackTracePrinter stackTracePrinter, ContextPairs contextPairs,
			ThrowableProxyConverter throwableProxyConverter,
			@Nullable StructuredLoggingJsonMembersCustomizer<?> customizer) {
		super((members) -> jsonMembers(stackTracePrinter, contextPairs, throwableProxyConverter, members), customizer);
	}

	private static void jsonMembers(@Nullable StackTracePrinter stackTracePrinter, ContextPairs contextPairs,
			ThrowableProxyConverter throwableProxyConverter, JsonWriter.Members<ILoggingEvent> members) {
		Extractor extractor = new Extractor(stackTracePrinter, throwableProxyConverter);
		members.add("@timestamp", ILoggingEvent::getInstant).as(LogstashStructuredLogFormatter::asTimestamp);
		members.add("@version", "1");
		members.add("message", ILoggingEvent::getFormattedMessage);
		members.add("logger_name", ILoggingEvent::getLoggerName);
		members.add("thread_name", ILoggingEvent::getThreadName);
		members.add("level", ILoggingEvent::getLevel);
		members.add("level_value", ILoggingEvent::getLevel).as(Level::toInt);
		members.add().usingPairs(contextPairs.flat("_", (pairs) -> {
			pairs.addMapEntries(ILoggingEvent::getMDCPropertyMap);
			pairs.add(ILoggingEvent::getKeyValuePairs, keyValuePairExtractor);
		}));
		members.add("tags", ILoggingEvent::getMarkerList)
			.whenNotNull()
			.as(LogstashStructuredLogFormatter::getMarkers)
			.whenNotEmpty();
		Function<@Nullable ILoggingEvent, @Nullable Object> getThrowableProxy = (event) -> (event != null)
				? event.getThrowableProxy() : null;
		members.add("stack_trace", (event) -> event).whenNotNull(getThrowableProxy).as(extractor::stackTrace);
	}

	private static String asTimestamp(Instant instant) {
		OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
	}

	private static Set<String> getMarkers(List<Marker> markers) {
		Set<String> result = new LinkedHashSet<>();
		addMarkers(result, markers.iterator());
		return result;
	}

	private static void addMarkers(Set<String> result, Iterator<Marker> iterator) {
		while (iterator.hasNext()) {
			Marker marker = iterator.next();
			result.add(marker.getName());
			if (marker.hasReferences()) {
				addMarkers(result, marker.iterator());
			}
		}
	}

}
