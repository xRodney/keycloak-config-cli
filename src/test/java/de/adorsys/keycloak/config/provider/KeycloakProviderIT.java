/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.provider;

import de.adorsys.keycloak.config.properties.ImmutableKeycloakConfigProperties;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.resource.ManagementPermissions;
import de.adorsys.keycloak.config.test.util.KeycloakMock;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockserver.integration.ClientAndServer;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.time.Duration;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;

class KeycloakProviderIT {
    private static KeycloakConfigProperties defaultProperties;
    private static ClientAndServer mockServerClient;

    @BeforeAll
    static void beforeAll() throws MalformedURLException {
        mockServerClient = ClientAndServer.startClientAndServer();
        defaultProperties = ImmutableKeycloakConfigProperties.builder()
                .url("http://localhost:" + mockServerClient.getPort())
                .isSslVerify(false)
                .loginRealm("master")
                .grantType("password")
                .user("someuser")
                .password("somepassword")
                .clientSecret("somesecret")
                .clientId("someclientid")
                .version("@keycloak.version@")
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Nested
    class ResteasyReadTimeout  {
        @Test
        void run() {
            var keycloakProvider = new KeycloakProvider(ImmutableKeycloakConfigProperties.builder().from(defaultProperties)
                    .readTimeout(Duration.ofMillis(1))
                    .build());

            // very low read timeout leads inevitably to a read timeout, which in turn shows that the configuration is applied
            ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getInstance);
            assertNotNull(thrown.getCause());
            assertTrue(thrown.getCause() instanceof SocketTimeoutException);
            assertThat(thrown.getCause().getMessage(), matchesPattern(".*[Rr]ead timed out.*"));
        }
    }

    @Nested
    class ResteasyConnectTimeout  {
        @Test
        @Timeout(value = 2L)
        void run() throws MalformedURLException {
            var keycloakProvider = new KeycloakProvider(ImmutableKeycloakConfigProperties.builder().from(defaultProperties)
                    .url("https://10.255.255.1")
                    .connectTimeout(Duration.ofMillis(10))
                    .build());

            // connect timeout since IP is not reachable - test fails if it exceeds one second which in turn shows that
            // the configuration is applied
            ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getInstance);
            assertNotNull(thrown.getCause());
            assertTrue(thrown.getCause() instanceof ConnectTimeoutException);
            assertThat(thrown.getCause().getMessage(), matchesPattern(".*[Cc]onnect timed out.*"));
        }
    }

    @Nested
    class InvalidServerUrl  {
        @Test
        void run() {
            var keycloakProvider = new KeycloakProvider(defaultProperties);

            assertThrows(NotFoundException.class, keycloakProvider::getKeycloakVersion);
        }
    }

    @Nested
    class HttpProxySystemProperties  {

        @Test
        void testHttpProxy() {
            try {
                System.setProperty("http.proxyHost", "localhost");
                System.setProperty("http.proxyPort", "2");

                var keycloakProvider = new KeycloakProvider(defaultProperties);

                ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getKeycloakVersion);

                assertThat(thrown.getMessage(), matchesPattern(".+ Connect to localhost:2 .+ failed: .+"));
            } finally {
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
            }
        }
    }

    @Nested
    class HttpProxySpringProperties  {
        @Test
        void run() throws MalformedURLException {
            var keycloakProvider = new KeycloakProvider(ImmutableKeycloakConfigProperties.builder().from(defaultProperties)
                    .httpProxy("http://localhost:2")
                    .build());

            ProcessingException thrown = assertThrows(ProcessingException.class, keycloakProvider::getKeycloakVersion);

            assertThat(thrown.getMessage(), matchesPattern(".+ Connect to localhost:2 .+ failed: .+"));
        }
    }

    @Nested
    class GetCustomApiProxy  {
        @Test
        void run() {
            mockServerClient.when(request().withPath("/realms/master/protocol/openid-connect/token")).respond(KeycloakMock::grantToken);

            var keycloakProvider = new KeycloakProvider(defaultProperties);

            ManagementPermissions proxy = keycloakProvider.getCustomApiProxy(ManagementPermissions.class);
            assertNotNull(proxy);
        }
    }

    @Nested
    class GetCustomApiProxyInvalidUri  {
        @Test
        void run() throws MalformedURLException {
            var keycloakProvider = new KeycloakProvider(ImmutableKeycloakConfigProperties.builder().from(defaultProperties)
                    .url("http://crappy|url")
                    .build());

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> keycloakProvider.getCustomApiProxy(ManagementPermissions.class));
            assertNotNull(thrown.getCause());
            assertTrue(thrown.getCause() instanceof URISyntaxException);
        }
    }
}
