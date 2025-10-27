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

package org.springframework.boot.hibernate.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Settings to apply when configuring Hibernate.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class HibernateSettings {

	private @Nullable Supplier<String> ddlAuto;

	private @Nullable Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

	public HibernateSettings ddlAuto(@Nullable Supplier<String> ddlAuto) {
		this.ddlAuto = ddlAuto;
		return this;
	}

	public @Nullable String getDdlAuto() {
		return (this.ddlAuto != null) ? this.ddlAuto.get() : null;
	}

	public HibernateSettings hibernatePropertiesCustomizers(
			Collection<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
		this.hibernatePropertiesCustomizers = new ArrayList<>(hibernatePropertiesCustomizers);
		return this;
	}

	public @Nullable Collection<HibernatePropertiesCustomizer> getHibernatePropertiesCustomizers() {
		return this.hibernatePropertiesCustomizers;
	}

}
