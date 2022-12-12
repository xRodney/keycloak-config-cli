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

package de.adorsys.keycloak.config.mock;

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.*;
import de.adorsys.keycloak.config.test.util.KeycloakMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Cookie;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockserver.model.HttpRequest.request;

@QuarkusTest
class CookieMockIT extends AbstractImportTest {
    private MockServerClient mockServerClient;

    CookieMockIT() {
        this.resourcePath = "import-files/simple-realm";
    }

    @BeforeEach
    void beforeEach() throws MalformedURLException {
        mockServerClient = ClientAndServer.startClientAndServer();

        keycloakProvider.setProperties(ImmutableKeycloakConfigProperties.builder()
                .url(new URL("http://localhost:" + mockServerClient.getPort()))
                .isSslVerify(true)
                .loginRealm("master")
                .grantType("password")
                .user("someuser")
                .password("somepassword")
                .clientSecret("somesecret")
                .clientId("someclientid")
                .version("@keycloak.version@")
                .availabilityCheck(ImmutableKeycloakAvailabilityCheck.builder()
                        .isEnabled(false)
                        .timeout(Duration.ofSeconds(10))
                        .retryDelay(Duration.ofSeconds(20))
                        .build()
                )
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build()
        );

        configPropertiesProvider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .remoteState(ImmutableImportRemoteStateProperties.builder().from(config.getRemoteState())
                        .isEnabled(false)
                        .build()
                )
                .cache(ImmutableImportCacheProperties.builder().from(config.getCache())
                        .isEnabled(false)
                        .build()
                )
                .build()
        );
    }

    @Test
    void run() throws Exception {
        mockServerClient.when(request().withPath("/realms/master/protocol/openid-connect/token")).respond(KeycloakMock::grantToken);
        mockServerClient.when(request().withPath("/admin/realms/simple")).respond(KeycloakMock::realm);
        mockServerClient.when(request().withPath("/admin/realms/simple/default-default-client-scopes")).respond(KeycloakMock::emptyList);
        mockServerClient.when(request().withPath("/admin/realms/simple/default-optional-client-scopes")).respond(KeycloakMock::emptyList);
        mockServerClient.when(request().withPath("/admin/realms/simple/identity-provider/instances")).respond(KeycloakMock::emptyList);
        mockServerClient.when(request().withPath("/realms/master/protocol/openid-connect/logout")).respond(KeycloakMock::noContent);

        mockServerClient.when(request().withPath("/admin/serverinfo")).respond((request) -> {
            assertThat(request.getCookieList(), hasSize(2));
            assertThat(request.getCookieList(), containsInAnyOrder(
                    new Cookie("key_expires", "value"),
                    new Cookie("key", "value")
            ));

            return KeycloakMock.serverInfo(request);
        });

        RealmImport realmImport = getFirstImport("00_create_simple-realm.json");
        realmImportService.doImport(realmImport);
    }
}
