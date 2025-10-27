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

package org.springframework.boot.info;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.security.auth.x500.X500Principal;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.info.SslInfo.CertificateValidityInfo.Status;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Information about the certificates that the application uses.
 *
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class SslInfo {

	private final SslBundles sslBundles;

	private final Clock clock;

	/**
	 * Creates a new instance.
	 * @param sslBundles the {@link SslBundles} to extract the info from
	 * @since 4.0.0
	 */
	public SslInfo(SslBundles sslBundles) {
		this(sslBundles, Clock.systemDefaultZone());
	}

	/**
	 * Creates a new instance.
	 * @param sslBundles the {@link SslBundles} to extract the info from
	 * @param clock the {@link Clock} to use
	 * @since 4.0.0
	 */
	public SslInfo(SslBundles sslBundles, Clock clock) {
		this.sslBundles = sslBundles;
		this.clock = clock;
	}

	/**
	 * Returns information on all SSL bundles.
	 * @return information on all SSL bundles
	 */
	public List<BundleInfo> getBundles() {
		return this.sslBundles.getBundleNames()
			.stream()
			.map((name) -> new BundleInfo(name, this.sslBundles.getBundle(name)))
			.toList();
	}

	/**
	 * Returns an SSL bundle by name.
	 * @param name the name of the SSL bundle
	 * @return the {@link BundleInfo} for the given SSL bundle
	 * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
	 * @since 3.5.0
	 */
	public BundleInfo getBundle(String name) {
		SslBundle bundle = this.sslBundles.getBundle(name);
		return new BundleInfo(name, bundle);
	}

	/**
	 * Info about a single {@link SslBundle}.
	 */
	public final class BundleInfo {

		private final String name;

		private final List<CertificateChainInfo> certificateChains;

		private BundleInfo(String name, SslBundle sslBundle) {
			this.name = name;
			this.certificateChains = extractCertificateChains(sslBundle.getStores().getKeyStore());
		}

		private List<CertificateChainInfo> extractCertificateChains(@Nullable KeyStore keyStore) {
			if (keyStore == null) {
				return Collections.emptyList();
			}
			try {
				return Collections.list(keyStore.aliases())
					.stream()
					.map((alias) -> new CertificateChainInfo(keyStore, alias))
					.toList();
			}
			catch (KeyStoreException ex) {
				return Collections.emptyList();
			}
		}

		public String getName() {
			return this.name;
		}

		public List<CertificateChainInfo> getCertificateChains() {
			return this.certificateChains;
		}

	}

	/**
	 * Info about a single certificate chain.
	 */
	public final class CertificateChainInfo {

		private final String alias;

		private final List<CertificateInfo> certificates;

		CertificateChainInfo(KeyStore keyStore, String alias) {
			this.alias = alias;
			this.certificates = extractCertificates(keyStore, alias);
		}

		private List<CertificateInfo> extractCertificates(KeyStore keyStore, String alias) {
			try {
				Certificate[] certificates = keyStore.getCertificateChain(alias);
				return (!ObjectUtils.isEmpty(certificates))
						? Arrays.stream(certificates).map(CertificateInfo::new).toList() : Collections.emptyList();
			}
			catch (KeyStoreException ex) {
				return Collections.emptyList();
			}
		}

		public String getAlias() {
			return this.alias;
		}

		public List<CertificateInfo> getCertificates() {
			return this.certificates;
		}

	}

	/**
	 * Info about a certificate.
	 */
	public final class CertificateInfo {

		private final @Nullable X509Certificate certificate;

		private CertificateInfo(Certificate certificate) {
			this.certificate = (certificate instanceof X509Certificate x509Certificate) ? x509Certificate : null;
		}

		public @Nullable String getSubject() {
			return extract(X509Certificate::getSubjectX500Principal, X500Principal::getName);
		}

		public @Nullable String getIssuer() {
			return extract(X509Certificate::getIssuerX500Principal, X500Principal::getName);
		}

		public @Nullable String getSerialNumber() {
			return extract(X509Certificate::getSerialNumber, (serial) -> serial.toString(16));
		}

		public @Nullable String getVersion() {
			return extract((certificate) -> "V" + certificate.getVersion());
		}

		public @Nullable String getSignatureAlgorithmName() {
			return extract(X509Certificate::getSigAlgName);
		}

		public @Nullable Instant getValidityStarts() {
			return extract(X509Certificate::getNotBefore, Date::toInstant);
		}

		public @Nullable Instant getValidityEnds() {
			return extract(X509Certificate::getNotAfter, Date::toInstant);
		}

		public @Nullable CertificateValidityInfo getValidity() {
			return extract((certificate) -> {
				Instant starts = getValidityStarts();
				Instant ends = getValidityEnds();
				Assert.state(starts != null, "Validity start not found");
				Assert.state(ends != null, "Validity end not found");
				CertificateValidityInfo.Status validity = checkValidity(starts, ends);
				return switch (validity) {
					case VALID -> CertificateValidityInfo.VALID;
					case EXPIRED -> new CertificateValidityInfo(Status.EXPIRED, "Not valid after %s", ends);
					case NOT_YET_VALID ->
						new CertificateValidityInfo(Status.NOT_YET_VALID, "Not valid before %s", starts);
				};
			});
		}

		private CertificateValidityInfo.Status checkValidity(Instant starts, Instant ends) {
			Instant now = SslInfo.this.clock.instant();
			if (now.isBefore(starts)) {
				return CertificateValidityInfo.Status.NOT_YET_VALID;
			}
			if (now.isAfter(ends)) {
				return CertificateValidityInfo.Status.EXPIRED;
			}
			return CertificateValidityInfo.Status.VALID;
		}

		private <V, R> @Nullable R extract(Function<X509Certificate, V> valueExtractor,
				Function<V, R> resultExtractor) {
			return extract(valueExtractor.andThen(resultExtractor));
		}

		private <R> @Nullable R extract(Function<X509Certificate, R> extractor) {
			return (this.certificate != null) ? extractor.apply(this.certificate) : null;
		}

	}

	/**
	 * Certificate validity info.
	 */
	public static class CertificateValidityInfo {

		static final CertificateValidityInfo VALID = new CertificateValidityInfo(Status.VALID, null);

		private final Status status;

		private final @Nullable String message;

		CertificateValidityInfo(Status status, @Nullable String message, Object... messageArgs) {
			this.status = status;
			this.message = (message != null) ? message.formatted(messageArgs) : null;
		}

		public Status getStatus() {
			return this.status;
		}

		public @Nullable String getMessage() {
			return this.message;
		}

		/**
		 * Validity Status.
		 */
		public enum Status {

			/**
			 * The certificate is valid.
			 */
			VALID(true),

			/**
			 * The certificate's validity date range is in the future.
			 */
			NOT_YET_VALID(false),

			/**
			 * The certificate's validity date range is in the past.
			 */
			EXPIRED(false);

			private final boolean valid;

			Status(boolean valid) {
				this.valid = valid;
			}

			public boolean isValid() {
				return this.valid;
			}

		}

	}

}
