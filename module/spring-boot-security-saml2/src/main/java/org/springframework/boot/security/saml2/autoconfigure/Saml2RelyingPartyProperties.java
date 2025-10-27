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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

/**
 * SAML2 relying party properties.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author Lasse Wulff
 * @since 4.0.0
 */
@ConfigurationProperties("spring.security.saml2.relyingparty")
public class Saml2RelyingPartyProperties {

	/**
	 * SAML2 relying party registrations.
	 */
	private final Map<String, Registration> registration = new LinkedHashMap<>();

	public Map<String, Registration> getRegistration() {
		return this.registration;
	}

	/**
	 * Represents a SAML Relying Party.
	 */
	public static class Registration {

		/**
		 * Relying party's entity ID. The value may contain a number of placeholders. They
		 * are "baseUrl", "registrationId", "baseScheme", "baseHost", and "basePort".
		 */
		private String entityId = "{baseUrl}/saml2/service-provider-metadata/{registrationId}";

		/**
		 * Assertion Consumer Service.
		 */
		private final Acs acs = new Acs();

		private final Signing signing = new Signing();

		private final Decryption decryption = new Decryption();

		private final Singlelogout singlelogout = new Singlelogout();

		/**
		 * Remote SAML Identity Provider.
		 */
		private final AssertingParty assertingparty = new AssertingParty();

		/**
		 * Name ID format for a relying party registration.
		 */
		private @Nullable String nameIdFormat;

		public String getEntityId() {
			return this.entityId;
		}

		public void setEntityId(String entityId) {
			this.entityId = entityId;
		}

		public Acs getAcs() {
			return this.acs;
		}

		public Signing getSigning() {
			return this.signing;
		}

		public Decryption getDecryption() {
			return this.decryption;
		}

		public Singlelogout getSinglelogout() {
			return this.singlelogout;
		}

		public AssertingParty getAssertingparty() {
			return this.assertingparty;
		}

		public @Nullable String getNameIdFormat() {
			return this.nameIdFormat;
		}

		public void setNameIdFormat(@Nullable String nameIdFormat) {
			this.nameIdFormat = nameIdFormat;
		}

		public static class Acs {

			/**
			 * Assertion Consumer Service location template. Can generate its location
			 * based on possible variables of "baseUrl", "registrationId", "baseScheme",
			 * "baseHost", and "basePort".
			 */
			private String location = "{baseUrl}/login/saml2/sso/{registrationId}";

			/**
			 * Assertion Consumer Service binding.
			 */
			private Saml2MessageBinding binding = Saml2MessageBinding.POST;

			public String getLocation() {
				return this.location;
			}

			public void setLocation(String location) {
				this.location = location;
			}

			public Saml2MessageBinding getBinding() {
				return this.binding;
			}

			public void setBinding(Saml2MessageBinding binding) {
				this.binding = binding;
			}

		}

		public static class Signing {

			/**
			 * Credentials used for signing the SAML authentication request.
			 */
			private List<Credential> credentials = new ArrayList<>();

			public List<Credential> getCredentials() {
				return this.credentials;
			}

			public void setCredentials(List<Credential> credentials) {
				this.credentials = credentials;
			}

			public static class Credential {

				/**
				 * Private key used for signing.
				 */
				private @Nullable Resource privateKeyLocation;

				/**
				 * Relying Party X509Certificate shared with the identity provider.
				 */
				private @Nullable Resource certificateLocation;

				public @Nullable Resource getPrivateKeyLocation() {
					return this.privateKeyLocation;
				}

				public void setPrivateKeyLocation(@Nullable Resource privateKey) {
					this.privateKeyLocation = privateKey;
				}

				public @Nullable Resource getCertificateLocation() {
					return this.certificateLocation;
				}

				public void setCertificateLocation(@Nullable Resource certificate) {
					this.certificateLocation = certificate;
				}

			}

		}

	}

	public static class Decryption {

		/**
		 * Credentials used for decrypting the SAML authentication request.
		 */
		private List<Credential> credentials = new ArrayList<>();

		public List<Credential> getCredentials() {
			return this.credentials;
		}

		public void setCredentials(List<Credential> credentials) {
			this.credentials = credentials;
		}

		public static class Credential {

			/**
			 * Private key used for decrypting.
			 */
			private @Nullable Resource privateKeyLocation;

			/**
			 * Relying Party X509Certificate shared with the identity provider.
			 */
			private @Nullable Resource certificateLocation;

			public @Nullable Resource getPrivateKeyLocation() {
				return this.privateKeyLocation;
			}

			public void setPrivateKeyLocation(@Nullable Resource privateKey) {
				this.privateKeyLocation = privateKey;
			}

			public @Nullable Resource getCertificateLocation() {
				return this.certificateLocation;
			}

			public void setCertificateLocation(@Nullable Resource certificate) {
				this.certificateLocation = certificate;
			}

		}

	}

	/**
	 * Represents a remote Identity Provider.
	 */
	public static class AssertingParty {

		/**
		 * Unique identifier for the identity provider.
		 */
		private @Nullable String entityId;

		/**
		 * URI to the metadata endpoint for discovery-based configuration.
		 */
		private @Nullable String metadataUri;

		private final Singlesignon singlesignon = new Singlesignon();

		private final Verification verification = new Verification();

		private final Singlelogout singlelogout = new Singlelogout();

		public @Nullable String getEntityId() {
			return this.entityId;
		}

		public void setEntityId(@Nullable String entityId) {
			this.entityId = entityId;
		}

		public @Nullable String getMetadataUri() {
			return this.metadataUri;
		}

		public void setMetadataUri(@Nullable String metadataUri) {
			this.metadataUri = metadataUri;
		}

		public Singlesignon getSinglesignon() {
			return this.singlesignon;
		}

		public Verification getVerification() {
			return this.verification;
		}

		public Singlelogout getSinglelogout() {
			return this.singlelogout;
		}

		/**
		 * Single sign on details for an Identity Provider.
		 */
		public static class Singlesignon {

			/**
			 * Remote endpoint to send authentication requests to.
			 */
			private @Nullable String url;

			/**
			 * Whether to redirect or post authentication requests.
			 */
			private @Nullable Saml2MessageBinding binding;

			/**
			 * Whether to sign authentication requests.
			 */
			private @Nullable Boolean signRequest;

			public @Nullable String getUrl() {
				return this.url;
			}

			public void setUrl(@Nullable String url) {
				this.url = url;
			}

			public @Nullable Saml2MessageBinding getBinding() {
				return this.binding;
			}

			public void setBinding(@Nullable Saml2MessageBinding binding) {
				this.binding = binding;
			}

			public @Nullable Boolean getSignRequest() {
				return this.signRequest;
			}

			public void setSignRequest(@Nullable Boolean signRequest) {
				this.signRequest = signRequest;
			}

		}

		/**
		 * Verification details for an Identity Provider.
		 */
		public static class Verification {

			/**
			 * Credentials used for verification of incoming SAML messages.
			 */
			private List<Credential> credentials = new ArrayList<>();

			public List<Credential> getCredentials() {
				return this.credentials;
			}

			public void setCredentials(List<Credential> credentials) {
				this.credentials = credentials;
			}

			public static class Credential {

				/**
				 * Locations of the X.509 certificate used for verification of incoming
				 * SAML messages.
				 */
				private @Nullable Resource certificate;

				public @Nullable Resource getCertificateLocation() {
					return this.certificate;
				}

				public void setCertificateLocation(@Nullable Resource certificate) {
					this.certificate = certificate;
				}

			}

		}

	}

	/**
	 * Single logout details.
	 */
	public static class Singlelogout {

		/**
		 * Location where SAML2 LogoutRequest gets sent to.
		 */
		private @Nullable String url;

		/**
		 * Location where SAML2 LogoutResponse gets sent to.
		 */
		private @Nullable String responseUrl;

		/**
		 * Whether to redirect or post logout requests.
		 */
		private @Nullable Saml2MessageBinding binding;

		public @Nullable String getUrl() {
			return this.url;
		}

		public void setUrl(@Nullable String url) {
			this.url = url;
		}

		public @Nullable String getResponseUrl() {
			return this.responseUrl;
		}

		public void setResponseUrl(@Nullable String responseUrl) {
			this.responseUrl = responseUrl;
		}

		public @Nullable Saml2MessageBinding getBinding() {
			return this.binding;
		}

		public void setBinding(@Nullable Saml2MessageBinding binding) {
			this.binding = binding;
		}

	}

}
