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

package org.springframework.boot.security.saml2.autoconfigure;

import java.io.InputStream;
import java.util.List;

import jakarta.servlet.Filter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CompositeFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Saml2RelyingPartyAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Moritz Halbritter
 * @author Lasse Lindqvist
 * @author Scott Frederick
 */
class Saml2RelyingPartyAutoConfigurationTests {

	private static final String PREFIX = "spring.security.saml2.relyingparty.registration";

	private static final String MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN = "managementSecurityFilterChain";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Saml2RelyingPartyAutoConfiguration.class,
				SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class));

	@Test
	void autoConfigurationShouldBeConditionalOnRelyingPartyRegistrationRepositoryClass() {
		this.contextRunner.withPropertyValues(getPropertyValues())
			.withClassLoader(new FilteredClassLoader(
					"org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository"))
			.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void autoConfigurationShouldBeConditionalOnServletWebApplication() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(Saml2RelyingPartyAutoConfiguration.class))
			.withPropertyValues(getPropertyValues())
			.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void relyingPartyRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void relyingPartyRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner.withPropertyValues(getPropertyValues()).run((context) -> {
			RelyingPartyRegistrationRepository repository = context.getBean(RelyingPartyRegistrationRepository.class);
			RelyingPartyRegistration registration = repository.findByRegistrationId("foo");

			assertThat(registration.getAssertingPartyMetadata().getSingleSignOnServiceLocation())
				.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php");
			assertThat(registration.getAssertingPartyMetadata().getEntityId())
				.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php");
			assertThat(registration.getAssertionConsumerServiceLocation())
				.isEqualTo("{baseUrl}/login/saml2/foo-entity-id");
			assertThat(registration.getAssertionConsumerServiceBinding()).isEqualTo(Saml2MessageBinding.REDIRECT);
			assertThat(registration.getAssertingPartyMetadata().getSingleSignOnServiceBinding())
				.isEqualTo(Saml2MessageBinding.POST);
			assertThat(registration.getAssertingPartyMetadata().getWantAuthnRequestsSigned()).isFalse();
			assertThat(registration.getSigningX509Credentials()).hasSize(1);
			assertThat(registration.getDecryptionX509Credentials()).hasSize(1);
			assertThat(registration.getAssertingPartyMetadata().getVerificationX509Credentials()).isNotNull();
			assertThat(registration.getEntityId()).isEqualTo("{baseUrl}/saml2/foo-entity-id");
			assertThat(registration.getSingleLogoutServiceLocation())
				.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SLOService.php");
			assertThat(registration.getSingleLogoutServiceResponseLocation())
				.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/");
			assertThat(registration.getSingleLogoutServiceBinding()).isEqualTo(Saml2MessageBinding.POST);
			assertThat(registration.getAssertingPartyMetadata().getSingleLogoutServiceLocation())
				.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SLOService.php");
			assertThat(registration.getAssertingPartyMetadata().getSingleLogoutServiceResponseLocation())
				.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/");
			assertThat(registration.getAssertingPartyMetadata().getSingleLogoutServiceBinding())
				.isEqualTo(Saml2MessageBinding.POST);
		});
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void autoConfigurationWhenSignRequestsTrueAndNoSigningCredentialsShouldThrowException() {
		this.contextRunner.withPropertyValues(getPropertyValuesWithoutSigningCredentials(true)).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining(
					"Signing credentials must not be empty when authentication requests require signing.");
		});
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void autoConfigurationWhenSignRequestsFalseAndNoSigningCredentialsShouldNotThrowException() {
		this.contextRunner.withPropertyValues(getPropertyValuesWithoutSigningCredentials(false))
			.run((context) -> assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	@WithPackageResources("idp-metadata")
	void autoconfigurationShouldQueryAssertingPartyMetadataWhenMetadataUrlIsPresent() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			String metadataUrl = server.url("").toString();
			setupMockResponse(server, new ClassPathResource("idp-metadata"));
			this.contextRunner.withPropertyValues(PREFIX + ".foo.assertingparty.metadata-uri=" + metadataUrl)
				.run((context) -> {
					assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class);
					assertThat(server.getRequestCount()).isOne();
				});
		}
	}

	@Test
	@WithPackageResources("idp-metadata")
	void autoconfigurationShouldUseBindingFromMetadataUrlIfPresent() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			String metadataUrl = server.url("").toString();
			setupMockResponse(server, new ClassPathResource("idp-metadata"));
			this.contextRunner.withPropertyValues(PREFIX + ".foo.assertingparty.metadata-uri=" + metadataUrl)
				.run((context) -> {
					RelyingPartyRegistrationRepository repository = context
						.getBean(RelyingPartyRegistrationRepository.class);
					RelyingPartyRegistration registration = repository.findByRegistrationId("foo");
					assertThat(registration.getAssertingPartyMetadata().getSingleSignOnServiceBinding())
						.isEqualTo(Saml2MessageBinding.POST);
				});
		}
	}

	@Test
	@WithPackageResources("idp-metadata")
	void autoconfigurationWhenMetadataUrlAndPropertyPresentShouldUseBindingFromProperty() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			String metadataUrl = server.url("").toString();
			setupMockResponse(server, new ClassPathResource("idp-metadata"));
			this.contextRunner
				.withPropertyValues(PREFIX + ".foo.assertingparty.metadata-uri=" + metadataUrl,
						PREFIX + ".foo.assertingparty.singlesignon.binding=redirect")
				.run((context) -> {
					RelyingPartyRegistrationRepository repository = context
						.getBean(RelyingPartyRegistrationRepository.class);
					RelyingPartyRegistration registration = repository.findByRegistrationId("foo");
					assertThat(registration.getAssertingPartyMetadata().getSingleSignOnServiceBinding())
						.isEqualTo(Saml2MessageBinding.REDIRECT);
				});
		}
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void autoconfigurationWhenNoMetadataUrlOrPropertyPresentShouldUseRedirectBinding() {
		this.contextRunner.withPropertyValues(getPropertyValuesWithoutSsoBinding()).run((context) -> {
			RelyingPartyRegistrationRepository repository = context.getBean(RelyingPartyRegistrationRepository.class);
			RelyingPartyRegistration registration = repository.findByRegistrationId("foo");
			assertThat(registration.getAssertingPartyMetadata().getSingleSignOnServiceBinding())
				.isEqualTo(Saml2MessageBinding.REDIRECT);
		});
	}

	@Test
	void relyingPartyRegistrationRepositoryShouldBeConditionalOnMissingBean() {
		this.contextRunner.withPropertyValues(getPropertyValues())
			.withUserConfiguration(RegistrationRepositoryConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class);
				assertThat(context).hasBean("testRegistrationRepository");
			});
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void samlLoginShouldBeConfigured() {
		this.contextRunner.withPropertyValues(getPropertyValues())
			.run((context) -> assertThat(hasSecurityFilter(context, Saml2WebSsoAuthenticationFilter.class)).isTrue());
	}

	@Test
	@WithPackageResources({ "private-key-location", "certificate-location" })
	void samlLoginShouldBackOffWhenASecurityFilterChainBeanIsPresent() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
			.withUserConfiguration(TestSecurityFilterChainConfig.class)
			.withPropertyValues(getPropertyValues())
			.run((context) -> assertThat(hasSecurityFilter(context, Saml2WebSsoAuthenticationFilter.class)).isFalse());
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void samlLoginShouldShouldBeConditionalOnSecurityWebFilterClass() {
		this.contextRunner
			.withClassLoader(
					new FilteredClassLoader(Thread.currentThread().getContextClassLoader(), SecurityFilterChain.class))
			.withPropertyValues(getPropertyValues())
			.run((context) -> assertThat(context).doesNotHaveBean(SecurityFilterChain.class));
	}

	@Test
	@WithPackageResources({ "certificate-location", "private-key-location" })
	void samlLogoutShouldBeConfigured() {
		this.contextRunner.withPropertyValues(getPropertyValues())
			.run((context) -> assertThat(hasSecurityFilter(context, Saml2LogoutRequestFilter.class)).isTrue());
	}

	private String[] getPropertyValuesWithoutSigningCredentials(boolean signRequests) {
		return new String[] { PREFIX
				+ ".foo.assertingparty.singlesignon.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				PREFIX + ".foo.assertingparty.singlesignon.binding=post",
				PREFIX + ".foo.assertingparty.singlesignon.sign-request=" + signRequests,
				PREFIX + ".foo.assertingparty.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".foo.assertingparty.verification.credentials[0].certificate-location=classpath:certificate-location" };
	}

	@Test
	@WithPackageResources("idp-metadata-with-multiple-providers")
	void autoconfigurationWhenMultipleProvidersAndNoSpecifiedEntityId() throws Exception {
		testMultipleProviders(null, "https://idp.example.com/idp/shibboleth");
	}

	@Test
	@WithPackageResources("idp-metadata-with-multiple-providers")
	void autoconfigurationWhenMultipleProvidersAndSpecifiedEntityId() throws Exception {
		testMultipleProviders("https://idp.example.com/idp/shibboleth", "https://idp.example.com/idp/shibboleth");
		testMultipleProviders("https://idp2.example.com/idp/shibboleth", "https://idp2.example.com/idp/shibboleth");
	}

	@Test
	@WithPackageResources("idp-metadata")
	void signRequestShouldApplyIfMetadataUriIsSet() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			String metadataUrl = server.url("").toString();
			setupMockResponse(server, new ClassPathResource("idp-metadata"));
			this.contextRunner.withPropertyValues(PREFIX + ".foo.assertingparty.metadata-uri=" + metadataUrl,
					PREFIX + ".foo.assertingparty.singlesignon.sign-request=true",
					PREFIX + ".foo.signing.credentials[0].private-key-location=classpath:org/springframework/boot/security/saml2/autoconfigure/rsa.key",
					PREFIX + ".foo.signing.credentials[0].certificate-location=classpath:org/springframework/boot/security/saml2/autoconfigure/rsa.crt")
				.run((context) -> {
					RelyingPartyRegistrationRepository repository = context
						.getBean(RelyingPartyRegistrationRepository.class);
					RelyingPartyRegistration registration = repository.findByRegistrationId("foo");
					assertThat(registration.getAssertingPartyMetadata().getWantAuthnRequestsSigned()).isTrue();
				});
		}
	}

	@Test
	@WithPackageResources("certificate-location")
	void autoconfigurationWithInvalidPrivateKeyShouldFail() {
		this.contextRunner.withPropertyValues(
				PREFIX + ".foo.signing.credentials[0].private-key-location=classpath:certificate-location",
				PREFIX + ".foo.signing.credentials[0].certificate-location=classpath:certificate-location",
				PREFIX + ".foo.assertingparty.singlesignon.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				PREFIX + ".foo.assertingparty.singlesignon.binding=post",
				PREFIX + ".foo.assertingparty.singlesignon.sign-request=false",
				PREFIX + ".foo.assertingparty.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".foo.assertingparty.verification.credentials[0].certificate-location=classpath:certificate-location")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("Missing private key or unrecognized format"));
	}

	@Test
	@WithPackageResources("private-key-location")
	void autoconfigurationWithInvalidCertificateShouldFail() {
		this.contextRunner.withPropertyValues(
				PREFIX + ".foo.signing.credentials[0].private-key-location=classpath:private-key-location",
				PREFIX + ".foo.signing.credentials[0].certificate-location=classpath:private-key-location",
				PREFIX + ".foo.assertingparty.singlesignon.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				PREFIX + ".foo.assertingparty.singlesignon.binding=post",
				PREFIX + ".foo.assertingparty.singlesignon.sign-request=false",
				PREFIX + ".foo.assertingparty.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".foo.assertingparty.verification.credentials[0].certificate-location=classpath:private-key-location")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("Missing certificates or unrecognized format"));
	}

	@Test
	@WithPackageResources("certificate-location")
	void causesManagementWebSecurityAutoConfigurationToBackOff() {
		WebApplicationContextRunner contextRunner = this.contextRunner.withConfiguration(
				AutoConfigurations.of(ManagementWebSecurityAutoConfiguration.class, WebMvcAutoConfiguration.class));
		assertThat(contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ManagementWebSecurityAutoConfiguration.class)));
		contextRunner.withPropertyValues(PREFIX
				+ ".simplesamlphp.assertingparty.single-sign-on.url=https://simplesaml-for-spring-saml/SSOService.php",
				PREFIX + ".simplesamlphp.assertingparty.single-sign-on.sign-request=false",
				PREFIX + ".simplesamlphp.assertingparty.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".simplesamlphp.assertingparty.verification.credentials[0].certificate-location=classpath:certificate-location")
			.run((context) -> assertThat(context).doesNotHaveBean(ManagementWebSecurityAutoConfiguration.class)
				.doesNotHaveBean(MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN));
	}

	private void testMultipleProviders(@Nullable String specifiedEntityId, String expected) throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.start();
			String metadataUrl = server.url("").toString();
			setupMockResponse(server, new ClassPathResource("idp-metadata-with-multiple-providers"));
			WebApplicationContextRunner contextRunner = this.contextRunner
				.withPropertyValues(PREFIX + ".foo.assertingparty.metadata-uri=" + metadataUrl);
			if (specifiedEntityId != null) {
				contextRunner = contextRunner
					.withPropertyValues(PREFIX + ".foo.assertingparty.entity-id=" + specifiedEntityId);
			}
			contextRunner.run((context) -> {
				assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class);
				assertThat(server.getRequestCount()).isOne();
				RelyingPartyRegistrationRepository repository = context
					.getBean(RelyingPartyRegistrationRepository.class);
				RelyingPartyRegistration registration = repository.findByRegistrationId("foo");
				assertThat(registration.getAssertingPartyMetadata().getEntityId()).isEqualTo(expected);
			});
		}
	}

	private String[] getPropertyValuesWithoutSsoBinding() {
		return new String[] { PREFIX
				+ ".foo.assertingparty.singlesignon.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				PREFIX + ".foo.assertingparty.singlesignon.sign-request=false",
				PREFIX + ".foo.assertingparty.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".foo.assertingparty.verification.credentials[0].certificate-location=classpath:certificate-location" };
	}

	private String[] getPropertyValues() {
		return new String[] {
				PREFIX + ".foo.signing.credentials[0].private-key-location=classpath:private-key-location",
				PREFIX + ".foo.signing.credentials[0].certificate-location=classpath:certificate-location",
				PREFIX + ".foo.decryption.credentials[0].private-key-location=classpath:private-key-location",
				PREFIX + ".foo.decryption.credentials[0].certificate-location=classpath:certificate-location",
				PREFIX + ".foo.singlelogout.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SLOService.php",
				PREFIX + ".foo.singlelogout.response-url=https://simplesaml-for-spring-saml.cfapps.io/",
				PREFIX + ".foo.singlelogout.binding=post",
				PREFIX + ".foo.assertingparty.singlesignon.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				PREFIX + ".foo.assertingparty.singlesignon.binding=post",
				PREFIX + ".foo.assertingparty.singlesignon.sign-request=false",
				PREFIX + ".foo.assertingparty.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".foo.assertingparty.verification.credentials[0].certificate-location=classpath:certificate-location",
				PREFIX + ".foo.asserting-party.singlelogout.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SLOService.php",
				PREFIX + ".foo.asserting-party.singlelogout.response-url=https://simplesaml-for-spring-saml.cfapps.io/",
				PREFIX + ".foo.asserting-party.singlelogout.binding=post",
				PREFIX + ".foo.entity-id={baseUrl}/saml2/foo-entity-id",
				PREFIX + ".foo.acs.location={baseUrl}/login/saml2/foo-entity-id",
				PREFIX + ".foo.acs.binding=redirect" };
	}

	private boolean hasSecurityFilter(AssertableWebApplicationContext context, Class<? extends Filter> filter) {
		return getSecurityFilterChain(context).getFilters().stream().anyMatch(filter::isInstance);
	}

	private SecurityFilterChain getSecurityFilterChain(AssertableWebApplicationContext context) {
		Filter springSecurityFilterChain = context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
		FilterChainProxy filterChainProxy = getFilterChainProxy(springSecurityFilterChain);
		SecurityFilterChain securityFilterChain = filterChainProxy.getFilterChains().get(0);
		return securityFilterChain;
	}

	private FilterChainProxy getFilterChainProxy(Filter filter) {
		if (filter instanceof FilterChainProxy filterChainProxy) {
			return filterChainProxy;
		}
		if (filter instanceof CompositeFilter) {
			List<?> filters = (List<?>) ReflectionTestUtils.getField(filter, "filters");
			assertThat(filters).isNotNull();
			return (FilterChainProxy) filters.stream()
				.filter(FilterChainProxy.class::isInstance)
				.findFirst()
				.orElseThrow();
		}
		throw new IllegalStateException("No FilterChainProxy found");
	}

	private void setupMockResponse(MockWebServer server, Resource resourceBody) throws Exception {
		try (InputStream metadataSource = resourceBody.getInputStream()) {
			try (Buffer metadataBuffer = new Buffer()) {
				metadataBuffer.readFrom(metadataSource);
				MockResponse metadataResponse = new MockResponse().setBody(metadataBuffer);
				server.enqueue(metadataResponse);
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class RegistrationRepositoryConfiguration {

		@Bean
		RelyingPartyRegistrationRepository testRegistrationRepository() {
			return mock(RelyingPartyRegistrationRepository.class);
		}

	}

	@EnableWebSecurity
	static class EnableWebSecurityConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TestSecurityFilterChainConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
			return http.securityMatcher("/**")
				.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
				.build();
		}

	}

}
