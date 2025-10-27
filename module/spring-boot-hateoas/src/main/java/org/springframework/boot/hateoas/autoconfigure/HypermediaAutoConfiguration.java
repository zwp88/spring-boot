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

package org.springframework.boot.hateoas.autoconfigure;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.http.MediaType;
import org.springframework.plugin.core.Plugin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring HATEOAS's
 * {@link EnableHypermediaSupport @EnableHypermediaSupport}.
 *
 * @author Roy Clarkson
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ EntityModel.class, RequestMapping.class, RequestMappingHandlerAdapter.class, Plugin.class,
		JsonMapper.class })
@ConditionalOnWebApplication
@EnableConfigurationProperties(HateoasProperties.class)
public final class HypermediaAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.hateoas.use-hal-as-default-json-media-type", matchIfMissing = true)
	HalConfiguration applicationJsonHalConfiguration() {
		return new HalConfiguration().withMediaType(MediaType.APPLICATION_JSON);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(LinkDiscoverers.class)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class HypermediaConfiguration {

	}

}
