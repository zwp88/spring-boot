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

package org.springframework.boot.test.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * AssertJ based JSON tester backed by Jackson 2. Usually instantiated via
 * {@link #initFields(Object, ObjectMapper)}, for example: <pre class="code">
 * public class ExampleObjectJsonTests {
 *
 *     private Jackson2Tester&lt;ExampleObject&gt; json;
 *
 *     &#064;Before
 *     public void setup() {
 *         ObjectMapper objectMapper = new ObjectMapper();
 *         Jackson2Tester.initFields(this, objectMapper);
 *     }
 *
 *     &#064;Test
 *     public void testWriteJson() throws IOException {
 *         ExampleObject object = //...
 *         assertThat(json.write(object)).isEqualToJson("expected.json");
 *     }
 *
 * }
 * </pre>
 *
 * See {@link AbstractJsonMarshalTester} for more details.
 *
 * @param <T> the type under test
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Diego Berrueta
 * @since 4.0.0
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of Jackson 3.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class Jackson2Tester<T> extends AbstractJsonMarshalTester<T> {

	private final ObjectMapper objectMapper;

	private @Nullable Class<?> view;

	/**
	 * Create a new {@link Jackson2Tester} instance.
	 * @param objectMapper the Jackson object mapper
	 */
	protected Jackson2Tester(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Create a new {@link Jackson2Tester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test
	 * @param objectMapper the Jackson object mapper
	 */
	public Jackson2Tester(Class<?> resourceLoadClass, ResolvableType type, ObjectMapper objectMapper) {
		this(resourceLoadClass, type, objectMapper, null);
	}

	/**
	 * Create a new {@link Jackson2Tester} instance.
	 * @param resourceLoadClass the source class used to load resources
	 * @param type the type under test
	 * @param objectMapper the Jackson object mapper
	 * @param view the JSON view
	 */
	public Jackson2Tester(Class<?> resourceLoadClass, ResolvableType type, ObjectMapper objectMapper,
			@Nullable Class<?> view) {
		super(resourceLoadClass, type);
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
		this.view = view;
	}

	@Override
	protected JsonContent<T> getJsonContent(String json) {
		Configuration configuration = Configuration.builder()
			.jsonProvider(new JacksonJsonProvider(this.objectMapper))
			.mappingProvider(new JacksonMappingProvider(this.objectMapper))
			.build();
		Class<?> resourceLoadClass = getResourceLoadClass();
		Assert.state(resourceLoadClass != null, "'resourceLoadClass' must not be null");
		return new JsonContent<>(resourceLoadClass, getType(), json, configuration);
	}

	@Override
	protected T readObject(InputStream inputStream, ResolvableType type) throws IOException {
		return getObjectReader(type).readValue(inputStream);
	}

	@Override
	protected T readObject(Reader reader, ResolvableType type) throws IOException {
		return getObjectReader(type).readValue(reader);
	}

	private ObjectReader getObjectReader(ResolvableType type) {
		ObjectReader objectReader = this.objectMapper.readerFor(getType(type));
		if (this.view != null) {
			return objectReader.withView(this.view);
		}
		return objectReader;
	}

	@Override
	protected String writeObject(T value, ResolvableType type) throws IOException {
		return getObjectWriter(type).writeValueAsString(value);
	}

	private ObjectWriter getObjectWriter(ResolvableType type) {
		ObjectWriter objectWriter = this.objectMapper.writerFor(getType(type));
		if (this.view != null) {
			return objectWriter.withView(this.view);
		}
		return objectWriter;
	}

	private JavaType getType(ResolvableType type) {
		return this.objectMapper.constructType(type.getType());
	}

	/**
	 * Utility method to initialize {@link Jackson2Tester} fields. See
	 * {@link Jackson2Tester class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param objectMapper the JSON mapper
	 * @see #initFields(Object, ObjectMapper)
	 */
	public static void initFields(Object testInstance, ObjectMapper objectMapper) {
		new Jackson2FieldInitializer().initFields(testInstance, objectMapper);
	}

	/**
	 * Utility method to initialize {@link Jackson2Tester} fields. See
	 * {@link Jackson2Tester class-level documentation} for example usage.
	 * @param testInstance the test instance
	 * @param objectMapperFactory a factory to create the object mapper
	 * @see #initFields(Object, ObjectMapper)
	 */
	public static void initFields(Object testInstance, ObjectFactory<ObjectMapper> objectMapperFactory) {
		new Jackson2FieldInitializer().initFields(testInstance, objectMapperFactory);
	}

	/**
	 * Returns a new instance of {@link Jackson2Tester} with the view that should be used
	 * for json serialization/deserialization.
	 * @param view the view class
	 * @return the new instance
	 */
	public Jackson2Tester<T> forView(Class<?> view) {
		Class<?> resourceLoadClass = getResourceLoadClass();
		ResolvableType type = getType();
		Assert.state(resourceLoadClass != null, "'resourceLoadClass' must not be null");
		Assert.state(type != null, "'type' must not be null");
		return new Jackson2Tester<>(resourceLoadClass, type, this.objectMapper, view);
	}

	/**
	 * {@link FieldInitializer} for Jackson.
	 */
	private static class Jackson2FieldInitializer extends FieldInitializer<ObjectMapper> {

		protected Jackson2FieldInitializer() {
			super(Jackson2Tester.class);
		}

		@Override
		protected AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type,
				ObjectMapper marshaller) {
			return new Jackson2Tester<>(resourceLoadClass, type, marshaller);
		}

	}

}
