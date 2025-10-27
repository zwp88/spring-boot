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

package org.springframework.boot.buildpack.platform.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * A {@link ProgressUpdateEvent} fired as an image is loaded.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class LoadImageUpdateEvent extends ProgressUpdateEvent {

	private final @Nullable String stream;

	private final @Nullable ErrorDetail errorDetail;

	@JsonCreator
	public LoadImageUpdateEvent(@Nullable String stream, String status, ProgressDetail progressDetail, String progress,
			@Nullable ErrorDetail errorDetail) {
		super(status, progressDetail, progress);
		this.stream = stream;
		this.errorDetail = errorDetail;
	}

	/**
	 * Return the stream response or {@code null} if no response is available.
	 * @return the stream response.
	 */
	public @Nullable String getStream() {
		return this.stream;
	}

	/**
	 * Return the error detail or {@code null} if no error occurred.
	 * @return the error detail, if any
	 * @since 3.2.12
	 */
	public @Nullable ErrorDetail getErrorDetail() {
		return this.errorDetail;
	}

	/**
	 * Details of an error embedded in a response stream.
	 *
	 * @since 3.2.12
	 */
	public static class ErrorDetail {

		private final String message;

		@JsonCreator
		public ErrorDetail(@JsonProperty("message") String message) {
			this.message = message;
		}

		/**
		 * Returns the message field from the error detail.
		 * @return the message
		 */
		public String getMessage() {
			return this.message;
		}

		@Override
		public String toString() {
			return this.message;
		}

	}

}
