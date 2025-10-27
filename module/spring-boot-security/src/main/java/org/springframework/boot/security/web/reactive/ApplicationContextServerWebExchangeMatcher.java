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

package org.springframework.boot.security.web.reactive;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link ApplicationContext} backed {@link ServerWebExchangeMatcher}. Can work directly
 * with the {@link ApplicationContext}, obtain an existing bean or
 * {@link AutowireCapableBeanFactory#createBean(Class) create a new bean} that is
 * autowired in the usual way.
 *
 * @param <C> the type of the context that the match method actually needs to use. Can be
 * an {@link ApplicationContext} or a class of an {@link ApplicationContext#getBean(Class)
 * existing bean}.
 * @author Madhura Bhave
 * @since 4.0.0
 */
public abstract class ApplicationContextServerWebExchangeMatcher<C> implements ServerWebExchangeMatcher {

	private final Class<? extends C> contextClass;

	private volatile @Nullable Supplier<C> context;

	private final Object contextLock = new Object();

	public ApplicationContextServerWebExchangeMatcher(Class<? extends C> contextClass) {
		Assert.notNull(contextClass, "'contextClass' must not be null");
		this.contextClass = contextClass;
	}

	@Override
	public final Mono<MatchResult> matches(ServerWebExchange exchange) {
		if (ignoreApplicationContext(exchange.getApplicationContext())) {
			return MatchResult.notMatch();
		}
		return matches(exchange, getContext(exchange));
	}

	/**
	 * Decides whether the rule implemented by the strategy matches the supplied exchange.
	 * @param exchange the source exchange
	 * @param context a supplier for the initialized context (may throw an exception)
	 * @return if the exchange matches
	 */
	protected abstract Mono<MatchResult> matches(ServerWebExchange exchange, Supplier<C> context);

	/**
	 * Returns if the {@link ApplicationContext} should be ignored and not used for
	 * matching. If this method returns {@code true} then the context will not be used and
	 * the {@link #matches(ServerWebExchange) matches} method will return {@code false}.
	 * @param applicationContext the candidate application context
	 * @return if the application context should be ignored
	 */
	protected boolean ignoreApplicationContext(@Nullable ApplicationContext applicationContext) {
		return false;
	}

	protected Supplier<C> getContext(ServerWebExchange exchange) {
		Supplier<C> context = this.context;
		if (context == null) {
			synchronized (this.contextLock) {
				context = this.context;
				if (context == null) {
					context = createContext(exchange);
					initialized(context);
					this.context = context;
				}
			}
		}
		return context;
	}

	/**
	 * Called once the context has been initialized.
	 * @param context a supplier for the initialized context (may throw an exception)
	 */
	protected void initialized(Supplier<C> context) {
	}

	@SuppressWarnings("unchecked")
	private Supplier<C> createContext(ServerWebExchange exchange) {
		ApplicationContext context = exchange.getApplicationContext();
		Assert.state(context != null, "No ApplicationContext found on ServerWebExchange.");
		if (this.contextClass.isInstance(context)) {
			return () -> (C) context;
		}
		return () -> context.getBean(this.contextClass);
	}

}
