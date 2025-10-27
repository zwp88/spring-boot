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

package org.springframework.boot.actuate.docs.liquibase;

import java.util.List;

import liquibase.changelog.ChangeSet.ExecType;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.liquibase.actuate.endpoint.LiquibaseEndpoint;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

/**
 * Tests for generating documentation describing the {@link LiquibaseEndpoint}.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(
		properties = "spring.liquibase.change-log=classpath:org/springframework/boot/actuate/docs/liquibase/db.changelog-master.yaml")
class LiquibaseEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void liquibase() {
		FieldDescriptor changeSetsField = fieldWithPath("contexts.*.liquibaseBeans.*.changeSets")
			.description("Change sets made by the Liquibase beans, keyed by bean name.");
		assertThat(this.mvc.get().uri("/actuator/liquibase")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("liquibase",
					responseFields(fieldWithPath("contexts").description("Application contexts keyed by id"),
							changeSetsField)
						.andWithPrefix("contexts.*.liquibaseBeans.*.changeSets[].", getChangeSetFieldDescriptors())
						.and(parentIdField())));
	}

	private List<FieldDescriptor> getChangeSetFieldDescriptors() {
		return List.of(fieldWithPath("author").description("Author of the change set."),
				fieldWithPath("changeLog").description("Change log that contains the change set."),
				fieldWithPath("comments").description("Comments on the change set."),
				fieldWithPath("contexts").description("Contexts of the change set."),
				fieldWithPath("dateExecuted").description("Timestamp of when the change set was executed."),
				fieldWithPath("deploymentId").description("ID of the deployment that ran the change set."),
				fieldWithPath("description").description("Description of the change set."),
				fieldWithPath("execType")
					.description("Execution type of the change set (" + describeEnumValues(ExecType.class) + ")."),
				fieldWithPath("id").description("ID of the change set."),
				fieldWithPath("labels").description("Labels associated with the change set."),
				fieldWithPath("checksum").description("Checksum of the change set."),
				fieldWithPath("orderExecuted").description("Order of the execution of the change set."),
				fieldWithPath("tag").description("Tag associated with the change set, if any.")
					.optional()
					.type(JsonFieldType.STRING));
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ EmbeddedDataSourceConfiguration.class, LiquibaseAutoConfiguration.class })
	static class TestConfiguration {

		@Bean
		LiquibaseEndpoint endpoint(ApplicationContext context) {
			return new LiquibaseEndpoint(context);
		}

	}

}
