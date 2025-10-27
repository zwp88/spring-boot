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

package org.springframework.boot.amqp.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.amqp.autoconfigure.RabbitProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link RabbitProperties} to {@link RabbitConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PropertiesRabbitConnectionDetails implements RabbitConnectionDetails {

	private final RabbitProperties properties;

	private final @Nullable SslBundles sslBundles;

	PropertiesRabbitConnectionDetails(RabbitProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public String getUsername() {
		return this.properties.determineUsername();
	}

	@Override
	public @Nullable String getPassword() {
		return this.properties.determinePassword();
	}

	@Override
	public @Nullable String getVirtualHost() {
		return this.properties.determineVirtualHost();
	}

	@Override
	public List<Address> getAddresses() {
		List<Address> addresses = new ArrayList<>();
		for (String address : this.properties.determineAddresses()) {
			int portSeparatorIndex = address.lastIndexOf(':');
			String host = address.substring(0, portSeparatorIndex);
			String port = address.substring(portSeparatorIndex + 1);
			addresses.add(new Address(host, Integer.parseInt(port)));
		}
		return addresses;
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		Ssl ssl = this.properties.getSsl();
		if (!ssl.determineEnabled()) {
			return null;
		}
		if (StringUtils.hasLength(ssl.getBundle())) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(ssl.getBundle());
		}
		return null;
	}

}
