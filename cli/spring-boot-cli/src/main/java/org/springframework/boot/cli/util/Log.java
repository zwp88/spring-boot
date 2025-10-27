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

package org.springframework.boot.cli.util;

import org.jspecify.annotations.Nullable;

/**
 * Simple logger used by the CLI.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public abstract class Log {

	private static @Nullable LogListener listener;

	public static void info(@Nullable String message) {
		System.out.println(message);
		if (listener != null) {
			listener.info(message);
		}
	}

	public static void infoPrint(@Nullable String message) {
		System.out.print(message);
		if (listener != null) {
			listener.infoPrint(message);
		}
	}

	public static void error(@Nullable String message) {
		System.err.println(message);
		if (listener != null) {
			listener.error(message);
		}
	}

	public static void error(Exception ex) {
		ex.printStackTrace(System.err);
		if (listener != null) {
			listener.error(ex);
		}
	}

	static void setListener(@Nullable LogListener listener) {
		Log.listener = listener;
	}

}
