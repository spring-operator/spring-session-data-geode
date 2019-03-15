/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.session.data.gemfire;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.config.annotation.CacheServerApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.session.Session;
import org.springframework.session.data.gemfire.config.annotation.web.http.EnableGemFireHttpSession;
import org.springframework.session.events.AbstractSessionEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * The ClientServerProxyRegionSessionOperationsIntegrationTests class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
	classes = ClientServerProxyRegionSessionOperationsIntegrationTests.SpringSessionDataGemFireClientConfiguration.class
)
public class ClientServerProxyRegionSessionOperationsIntegrationTests extends AbstractGemFireIntegrationTests {

	private static final int MAX_INACTIVE_INTERVAL_IN_SECONDS = 1;

	@Autowired @SuppressWarnings("all")
	private SessionEventListener sessionEventListener;

	@BeforeClass
	public static void startGemFireServer() throws IOException {
		startGemFireServer(SpringSessionDataGemFireServerConfiguration.class);
	}

	@Test
	public void createReadUpdateExpireRecreateDeleteRecreateSessionResultsCorrectSessionCreatedEvents() {

		Session session = save(touch(createSession()));

		assertValidSession(session);

		AbstractSessionEvent sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// GET
		Session loadedSession = get(session.getId());

		assertThat(loadedSession).isNotNull();
		assertThat(loadedSession.getId()).isEqualTo(session.getId());
		assertThat(loadedSession.getCreationTime()).isEqualTo(session.getCreationTime());
		assertThat(loadedSession.getLastAccessedTime().compareTo(session.getLastAccessedTime()))
			.isGreaterThanOrEqualTo(0);

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isNull();

		loadedSession.setAttribute("attrOne", 1);
		loadedSession.setAttribute("attrTwo", 2);

		// UPDATE
		save(touch(loadedSession));

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isNull();

		// EXPIRE
		sessionEvent = this.sessionEventListener.waitForSessionEvent(
			TimeUnit.SECONDS.toMillis(MAX_INACTIVE_INTERVAL_IN_SECONDS + 1));

		assertThat(sessionEvent).isInstanceOf(SessionExpiredEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// RECREATE
		save(touch(session));

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// DELETE
		delete(session);

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionDeletedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());

		// RECREATE
		save(touch(session));

		sessionEvent = this.sessionEventListener.waitForSessionEvent(500);

		assertThat(sessionEvent).isInstanceOf(SessionCreatedEvent.class);
		assertThat(sessionEvent.getSessionId()).isEqualTo(session.getId());
	}

	@ClientCacheApplication(
		logLevel = "error",
		pingInterval = 5000,
		readTimeout = 2000,
		retryAttempts = 1,
		subscriptionEnabled = true
	)
	@EnableGemFireHttpSession(poolName = "DEFAULT")
	@SuppressWarnings("unused")
	static class SpringSessionDataGemFireClientConfiguration {

		@Bean
		public SessionEventListener sessionEventListener() {
			return new SessionEventListener();
		}

	}

	@CacheServerApplication(
		name = "ClientServerProxyRegionSessionOperationsIntegrationTests",
		logLevel = "error"
	)
	@EnableGemFireHttpSession(maxInactiveIntervalInSeconds = MAX_INACTIVE_INTERVAL_IN_SECONDS)
	static class SpringSessionDataGemFireServerConfiguration {

		@SuppressWarnings("resource")
		public static void main(String[] args) throws IOException {

			AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(SpringSessionDataGemFireServerConfiguration.class);

			context.registerShutdownHook();
		}
	}
}
