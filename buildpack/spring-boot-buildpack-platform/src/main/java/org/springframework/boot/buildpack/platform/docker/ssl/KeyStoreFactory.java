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

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Utility methods for creating Java trust material from key and certificate files.
 *
 * @author Scott Frederick
 */
final class KeyStoreFactory {

	private static final char[] NO_PASSWORD = {};

	private KeyStoreFactory() {
	}

	/**
	 * Create a new {@link KeyStore} populated with the certificate stored at the
	 * specified file path and an optional private key.
	 * @param certPath the path to the certificate authority file
	 * @param keyPath the path to the private file
	 * @param alias the alias to use for KeyStore entries
	 * @return the {@code KeyStore}
	 */
	static KeyStore create(Path certPath, @Nullable Path keyPath, String alias) {
		try {
			KeyStore keyStore = getKeyStore();
			String certificateText = Files.readString(certPath);
			List<X509Certificate> certificates = PemCertificateParser.parse(certificateText);
			PrivateKey privateKey = getPrivateKey(keyPath);
			try {
				addCertificates(keyStore, certificates.toArray(X509Certificate[]::new), privateKey, alias);
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException("Error adding certificates to KeyStore: " + ex.getMessage(), ex);
			}
			return keyStore;
		}
		catch (GeneralSecurityException | IOException ex) {
			throw new IllegalStateException("Error creating KeyStore: " + ex.getMessage(), ex);
		}
	}

	private static KeyStore getKeyStore()
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null);
		return keyStore;
	}

	private static @Nullable PrivateKey getPrivateKey(@Nullable Path path) throws IOException {
		if (path != null && Files.exists(path)) {
			String text = Files.readString(path);
			return PemPrivateKeyParser.parse(text);
		}
		return null;
	}

	private static void addCertificates(KeyStore keyStore, X509Certificate[] certificates,
			@Nullable PrivateKey privateKey, String alias) throws KeyStoreException {
		if (privateKey != null) {
			keyStore.setKeyEntry(alias, privateKey, NO_PASSWORD, certificates);
		}
		else {
			for (int index = 0; index < certificates.length; index++) {
				keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
			}
		}
	}

}
