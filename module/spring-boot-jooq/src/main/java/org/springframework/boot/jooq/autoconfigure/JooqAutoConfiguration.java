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

package org.springframework.boot.jooq.autoconfigure;

import java.io.IOException;
import java.io.InputStream;

import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.TransactionProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for jOOQ.
 *
 * @author Andreas Ahlenstorf
 * @author Michael Simons
 * @author Dmytro Nosan
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class })
@ConditionalOnClass(DSLContext.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(JooqProperties.class)
public final class JooqAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ConnectionProvider.class)
	DataSourceConnectionProvider dataSourceConnectionProvider(DataSource dataSource) {
		return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource));
	}

	@Bean
	@ConditionalOnBean(PlatformTransactionManager.class)
	@ConditionalOnMissingBean(TransactionProvider.class)
	SpringTransactionProvider transactionProvider(PlatformTransactionManager txManager) {
		return new SpringTransactionProvider(txManager);
	}

	@Bean
	@Order(0)
	DefaultExecuteListenerProvider jooqExceptionTranslatorExecuteListenerProvider(
			ExceptionTranslatorExecuteListener exceptionTranslatorExecuteListener) {
		return new DefaultExecuteListenerProvider(exceptionTranslatorExecuteListener);
	}

	@Bean
	@ConditionalOnMissingBean
	ExceptionTranslatorExecuteListener jooqExceptionTranslator() {
		return ExceptionTranslatorExecuteListener.DEFAULT;
	}

	@Bean
	@ConditionalOnMissingBean(DSLContext.class)
	DefaultDSLContext dslContext(org.jooq.Configuration configuration) {
		return new DefaultDSLContext(configuration);
	}

	@Bean
	@ConditionalOnMissingBean(org.jooq.Configuration.class)
	DefaultConfiguration jooqConfiguration(JooqProperties properties, ConnectionProvider connectionProvider,
			DataSource dataSource, ObjectProvider<TransactionProvider> transactionProvider,
			ObjectProvider<ExecuteListenerProvider> executeListenerProviders,
			ObjectProvider<DefaultConfigurationCustomizer> configurationCustomizers,
			ObjectProvider<Settings> settingsProvider) {
		DefaultConfiguration configuration = new DefaultConfiguration();
		configuration.set(properties.determineSqlDialect(dataSource));
		configuration.set(connectionProvider);
		transactionProvider.ifAvailable(configuration::set);
		settingsProvider.ifAvailable(configuration::set);
		configuration.set(executeListenerProviders.orderedStream().toArray(ExecuteListenerProvider[]::new));
		configurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
		return configuration;
	}

	@Bean
	@ConditionalOnProperty("spring.jooq.config")
	@ConditionalOnMissingBean
	Settings settings(JooqProperties properties) throws IOException {
		if (!ClassUtils.isPresent("jakarta.xml.bind.JAXBContext", null)) {
			throw new JaxbNotAvailableException();
		}
		Resource resource = properties.getConfig();
		Assert.state(resource != null, "'resource' must not be null");
		Assert.state(resource.exists(),
				() -> "Resource %s set in spring.jooq.config does not exist".formatted(resource));
		try (InputStream stream = resource.getInputStream()) {
			return new JaxbSettingsLoader().load(stream);
		}
	}

	/**
	 * Load {@link Settings} with <a href=
	 * "https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxb-unmarshaller">
	 * XML External Entity Prevention</a>.
	 */
	private static final class JaxbSettingsLoader {

		private Settings load(InputStream inputStream) {
			try {
				SAXParser parser = createParserFactory().newSAXParser();
				Source source = new SAXSource(parser.getXMLReader(), new InputSource(inputStream));
				JAXBContext context = JAXBContext.newInstance(Settings.class);
				return context.createUnmarshaller().unmarshal(source, Settings.class).getValue();
			}
			catch (ParserConfigurationException | JAXBException | SAXException ex) {
				throw new IllegalStateException("Failed to unmarshal settings", ex);
			}
		}

		private SAXParserFactory createParserFactory()
				throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setNamespaceAware(true);
			factory.setXIncludeAware(false);
			return factory;
		}

	}

}
