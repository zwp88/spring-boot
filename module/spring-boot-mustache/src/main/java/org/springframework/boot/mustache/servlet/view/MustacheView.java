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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;

import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * Spring MVC {@link View} using the Mustache template engine.
 *
 * @author Brian Clozel
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0.0
 */
public class MustacheView extends AbstractTemplateView {

	private @Nullable Compiler compiler;

	private @Nullable String charset;

	/**
	 * Set the Mustache compiler to be used by this view.
	 * <p>
	 * Typically this property is not set directly. Instead a single {@link Compiler} is
	 * expected in the Spring application context which is used to compile Mustache
	 * templates.
	 * @param compiler the Mustache compiler
	 */
	public void setCompiler(Compiler compiler) {
		this.compiler = compiler;
	}

	/**
	 * Set the charset used for reading Mustache template files.
	 * @param charset the charset to use for reading template files
	 */
	public void setCharset(@Nullable String charset) {
		this.charset = charset;
	}

	@Override
	public boolean checkResource(Locale locale) throws Exception {
		Resource resource = getResource();
		return resource != null;
	}

	@Override
	protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Resource resource = getResource();
		Assert.state(resource != null, "'resource' must not be null");
		Template template = createTemplate(resource);
		if (template != null) {
			template.execute(model, response.getWriter());
		}
	}

	private @Nullable Resource getResource() {
		ApplicationContext applicationContext = getApplicationContext();
		String url = getUrl();
		if (applicationContext == null || url == null) {
			return null;
		}
		Resource resource = applicationContext.getResource(url);
		return (resource.exists()) ? resource : null;
	}

	private Template createTemplate(Resource resource) throws IOException {
		try (Reader reader = getReader(resource)) {
			Assert.state(this.compiler != null, "'compiler' must not be null");
			return this.compiler.compile(reader);
		}
	}

	private Reader getReader(Resource resource) throws IOException {
		if (this.charset != null) {
			return new InputStreamReader(resource.getInputStream(), this.charset);
		}
		return new InputStreamReader(resource.getInputStream());
	}

}
