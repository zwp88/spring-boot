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

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureWebTestClient
class AutoConfigureWebTestClientIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void codecsAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(CodecsAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
