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

package org.springframework.boot.ldap.autoconfigure;

import java.util.Collections;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.ldap.autoconfigure.LdapProperties.Template;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.ldap.convert.ConverterUtils;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for LDAP.
 *
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ContextSource.class)
@EnableConfigurationProperties(LdapProperties.class)
public final class LdapAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(LdapConnectionDetails.class)
	PropertiesLdapConnectionDetails propertiesLdapConnectionDetails(LdapProperties properties,
			Environment environment) {
		return new PropertiesLdapConnectionDetails(properties, environment);
	}

	@Bean
	@ConditionalOnMissingBean
	LdapContextSource ldapContextSource(LdapConnectionDetails connectionDetails, LdapProperties properties,
			ObjectProvider<DirContextAuthenticationStrategy> dirContextAuthenticationStrategy) {
		LdapContextSource source = new LdapContextSource();
		dirContextAuthenticationStrategy.ifUnique(source::setAuthenticationStrategy);
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(connectionDetails.getUsername()).to(source::setUserDn);
		propertyMapper.from(connectionDetails.getPassword()).to(source::setPassword);
		propertyMapper.from(properties.getAnonymousReadOnly()).to(source::setAnonymousReadOnly);
		propertyMapper.from(properties.getReferral())
			.as(((referral) -> referral.name().toLowerCase(Locale.ROOT)))
			.to(source::setReferral);
		propertyMapper.from(connectionDetails.getBase()).to(source::setBase);
		propertyMapper.from(connectionDetails.getUrls()).to(source::setUrls);
		propertyMapper.from(properties.getBaseEnvironment())
			.to((baseEnvironment) -> source.setBaseEnvironmentProperties(Collections.unmodifiableMap(baseEnvironment)));
		return source;
	}

	@Bean
	@ConditionalOnMissingBean
	ObjectDirectoryMapper objectDirectoryMapper() {
		ApplicationConversionService conversionService = new ApplicationConversionService();
		ConverterUtils.addDefaultConverters(conversionService);
		DefaultObjectDirectoryMapper objectDirectoryMapper = new DefaultObjectDirectoryMapper();
		objectDirectoryMapper.setConversionService(conversionService);
		return objectDirectoryMapper;
	}

	@Bean
	@ConditionalOnMissingBean(LdapOperations.class)
	LdapTemplate ldapTemplate(LdapProperties properties, ContextSource contextSource,
			ObjectDirectoryMapper objectDirectoryMapper) {
		Template template = properties.getTemplate();
		PropertyMapper propertyMapper = PropertyMapper.get();
		LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
		ldapTemplate.setObjectDirectoryMapper(objectDirectoryMapper);
		propertyMapper.from(template.isIgnorePartialResultException())
			.to(ldapTemplate::setIgnorePartialResultException);
		propertyMapper.from(template.isIgnoreNameNotFoundException()).to(ldapTemplate::setIgnoreNameNotFoundException);
		propertyMapper.from(template.isIgnoreSizeLimitExceededException())
			.to(ldapTemplate::setIgnoreSizeLimitExceededException);
		return ldapTemplate;
	}

}
