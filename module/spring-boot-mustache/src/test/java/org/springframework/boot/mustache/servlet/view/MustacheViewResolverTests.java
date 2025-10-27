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

package org.springframework.boot.mustache.servlet.view;

import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class MustacheViewResolverTests {

	private final MustacheViewResolver resolver = new MustacheViewResolver();

	@BeforeEach
	void init() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		this.resolver.setApplicationContext(applicationContext);
		this.resolver.setServletContext(new MockServletContext());
		this.resolver.setPrefix("classpath:");
		this.resolver.setSuffix(".html");
	}

	@Test
	void resolveNonExistent() throws Exception {
		assertThat(this.resolver.resolveViewName("bar", Locale.ROOT)).isNull();
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void resolveExisting() throws Exception {
		assertThat(this.resolver.resolveViewName("template", Locale.ROOT)).isNotNull();
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void setsContentType() throws Exception {
		this.resolver.setContentType("application/octet-stream");
		View view = this.resolver.resolveViewName("template", Locale.ROOT);
		assertThat(view).isNotNull();
		Assertions.assertThat(view.getContentType()).isEqualTo("application/octet-stream");

	}

}
