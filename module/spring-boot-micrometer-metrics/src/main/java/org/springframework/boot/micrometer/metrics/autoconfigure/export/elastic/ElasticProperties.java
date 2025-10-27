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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.elastic;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Elastic
 * metrics export.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("management.elastic.metrics.export")
public class ElasticProperties extends StepRegistryProperties {

	/**
	 * Host to export metrics to.
	 */
	private String host = "http://localhost:9200";

	/**
	 * Index to export metrics to.
	 */
	private String index = "micrometer-metrics";

	/**
	 * Index date format used for rolling indices. Appended to the index name.
	 */
	private String indexDateFormat = "yyyy-MM";

	/**
	 * Prefix to separate the index name from the date format used for rolling indices.
	 */
	private String indexDateSeparator = "-";

	/**
	 * Name of the timestamp field.
	 */
	private String timestampFieldName = "@timestamp";

	/**
	 * Whether to create the index automatically if it does not exist.
	 */
	private boolean autoCreateIndex = true;

	/**
	 * Login user of the Elastic server. Mutually exclusive with api-key-credentials.
	 */
	private @Nullable String userName;

	/**
	 * Login password of the Elastic server. Mutually exclusive with api-key-credentials.
	 */
	private @Nullable String password;

	/**
	 * Ingest pipeline name. By default, events are not pre-processed.
	 */
	private @Nullable String pipeline;

	/**
	 * Base64-encoded credentials string. Mutually exclusive with user-name and password.
	 */
	private @Nullable String apiKeyCredentials;

	/**
	 * Whether to enable _source in the default index template when auto-creating the
	 * index.
	 */
	private boolean enableSource = false;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getIndex() {
		return this.index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getIndexDateFormat() {
		return this.indexDateFormat;
	}

	public void setIndexDateFormat(String indexDateFormat) {
		this.indexDateFormat = indexDateFormat;
	}

	public String getIndexDateSeparator() {
		return this.indexDateSeparator;
	}

	public void setIndexDateSeparator(String indexDateSeparator) {
		this.indexDateSeparator = indexDateSeparator;
	}

	public String getTimestampFieldName() {
		return this.timestampFieldName;
	}

	public void setTimestampFieldName(String timestampFieldName) {
		this.timestampFieldName = timestampFieldName;
	}

	public boolean isAutoCreateIndex() {
		return this.autoCreateIndex;
	}

	public void setAutoCreateIndex(boolean autoCreateIndex) {
		this.autoCreateIndex = autoCreateIndex;
	}

	public @Nullable String getUserName() {
		return this.userName;
	}

	public void setUserName(@Nullable String userName) {
		this.userName = userName;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public @Nullable String getPipeline() {
		return this.pipeline;
	}

	public void setPipeline(@Nullable String pipeline) {
		this.pipeline = pipeline;
	}

	public @Nullable String getApiKeyCredentials() {
		return this.apiKeyCredentials;
	}

	public void setApiKeyCredentials(@Nullable String apiKeyCredentials) {
		this.apiKeyCredentials = apiKeyCredentials;
	}

	public boolean isEnableSource() {
		return this.enableSource;
	}

	public void setEnableSource(boolean enableSource) {
		this.enableSource = enableSource;
	}

}
