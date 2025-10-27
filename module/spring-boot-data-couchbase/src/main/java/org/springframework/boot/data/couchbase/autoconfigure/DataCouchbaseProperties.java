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

package org.springframework.boot.data.couchbase.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data Couchbase.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.couchbase")
public class DataCouchbaseProperties {

	/**
	 * Automatically create views and indexes. Use the meta-data provided by
	 * "@ViewIndexed", "@N1qlPrimaryIndexed" and "@N1qlSecondaryIndexed".
	 */
	private boolean autoIndex;

	/**
	 * Name of the bucket to connect to.
	 */
	private @Nullable String bucketName;

	/**
	 * Name of the scope used for all collection access.
	 */
	private @Nullable String scopeName;

	/**
	 * Fully qualified name of the FieldNamingStrategy to use.
	 */
	private @Nullable Class<?> fieldNamingStrategy;

	/**
	 * Name of the field that stores the type information for complex types when using
	 * "MappingCouchbaseConverter".
	 */
	private String typeKey = "_class";

	public boolean isAutoIndex() {
		return this.autoIndex;
	}

	public void setAutoIndex(boolean autoIndex) {
		this.autoIndex = autoIndex;
	}

	public @Nullable String getBucketName() {
		return this.bucketName;
	}

	public void setBucketName(@Nullable String bucketName) {
		this.bucketName = bucketName;
	}

	public @Nullable String getScopeName() {
		return this.scopeName;
	}

	public void setScopeName(@Nullable String scopeName) {
		this.scopeName = scopeName;
	}

	public @Nullable Class<?> getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	public void setFieldNamingStrategy(@Nullable Class<?> fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public String getTypeKey() {
		return this.typeKey;
	}

	public void setTypeKey(String typeKey) {
		this.typeKey = typeKey;
	}

}
