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

package org.springframework.boot.mail.autoconfigure;

import jakarta.mail.MessagingException;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto configuration} for testing mail service
 * connectivity on startup.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = MailSenderAutoConfiguration.class)
@ConditionalOnBooleanProperty("spring.mail.test-connection")
@ConditionalOnSingleCandidate(JavaMailSenderImpl.class)
public final class MailSenderValidatorAutoConfiguration {

	private final JavaMailSenderImpl mailSender;

	MailSenderValidatorAutoConfiguration(JavaMailSenderImpl mailSender) {
		this.mailSender = mailSender;
		validateConnection();
	}

	private void validateConnection() {
		try {
			this.mailSender.testConnection();
		}
		catch (MessagingException ex) {
			throw new IllegalStateException("Mail server is not available", ex);
		}
	}

}
