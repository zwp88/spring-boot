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

package org.springframework.boot.context.properties.source;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

/**
 * Simple cache that uses a {@link SoftReference} to cache a value for as long as
 * possible.
 *
 * @param <T> the value type
 * @author Phillip Webb
 */
class SoftReferenceConfigurationPropertyCache<T> implements ConfigurationPropertyCaching {

	private static final Duration UNLIMITED = Duration.ZERO;

	static final CacheOverride NO_OP_OVERRIDE = () -> {
	};

	private final boolean neverExpire;

	private volatile @Nullable Duration timeToLive;

	private volatile SoftReference<@Nullable T> value = new SoftReference<>(null);

	private volatile @Nullable Instant lastAccessed = now();

	SoftReferenceConfigurationPropertyCache(boolean neverExpire) {
		this.neverExpire = neverExpire;
	}

	@Override
	public void enable() {
		this.timeToLive = UNLIMITED;
	}

	@Override
	public void disable() {
		this.timeToLive = null;
	}

	@Override
	public void setTimeToLive(@Nullable Duration timeToLive) {
		this.timeToLive = (timeToLive == null || timeToLive.isZero()) ? null : timeToLive;
	}

	@Override
	public void clear() {
		this.lastAccessed = null;
	}

	@Override
	public CacheOverride override() {
		if (this.neverExpire) {
			return NO_OP_OVERRIDE;
		}
		ActiveCacheOverride override = new ActiveCacheOverride(this);
		if (override.timeToLive() == null) {
			// Ensure we don't use stale data on the first access
			clear();
		}
		this.timeToLive = UNLIMITED;
		return override;
	}

	void restore(ActiveCacheOverride override) {
		this.timeToLive = override.timeToLive();
		this.lastAccessed = override.lastAccessed();
	}

	/**
	 * Get a value from the cache, creating it if necessary.
	 * @param factory a factory used to create the item if there is no reference to it.
	 * @param refreshAction action called to refresh the value if it has expired
	 * @return the value from the cache
	 */
	T get(Supplier<T> factory, UnaryOperator<T> refreshAction) {
		T value = getValue();
		if (value == null) {
			value = refreshAction.apply(factory.get());
			setValue(value);
		}
		else if (hasExpired()) {
			value = refreshAction.apply(value);
			setValue(value);
		}
		if (!this.neverExpire) {
			this.lastAccessed = now();
		}
		return value;
	}

	private boolean hasExpired() {
		if (this.neverExpire) {
			return false;
		}
		Duration timeToLive = this.timeToLive;
		Instant lastAccessed = this.lastAccessed;
		if (timeToLive == null || lastAccessed == null) {
			return true;
		}
		return !UNLIMITED.equals(timeToLive) && now().isAfter(lastAccessed.plus(timeToLive));
	}

	protected Instant now() {
		return Instant.now();
	}

	protected @Nullable T getValue() {
		return this.value.get();
	}

	protected void setValue(T value) {
		this.value = new SoftReference<>(value);
	}

	/**
	 * An active {@link CacheOverride} with a stored time-to-live.
	 */
	private record ActiveCacheOverride(SoftReferenceConfigurationPropertyCache<?> cache, @Nullable Duration timeToLive,
			@Nullable Instant lastAccessed, AtomicBoolean active) implements CacheOverride {

		ActiveCacheOverride(SoftReferenceConfigurationPropertyCache<?> cache) {
			this(cache, cache.timeToLive, cache.lastAccessed, new AtomicBoolean());
		}

		@Override
		public void close() {
			if (active().compareAndSet(false, true)) {
				this.cache.restore(this);
			}
		}

	}

}
