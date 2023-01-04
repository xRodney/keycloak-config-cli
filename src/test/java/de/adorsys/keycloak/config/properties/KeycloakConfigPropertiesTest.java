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

package de.adorsys.keycloak.config.properties;

import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@ExtendWith(GithubActionsExtension.class)
@QuarkusTest
@TestProfile(KeycloakConfigPropertiesTest.TestConfiguration.class)
class KeycloakConfigPropertiesTest {

    @Inject
    private KeycloakConfigProperties properties;

    @Test
    void shouldPopulateConfigurationProperties() throws MalformedURLException {
        assertThat(properties.getLoginRealm(), is("moped"));
        assertThat(properties.getClientId(), is("moped-client"));
        assertThat(properties.getUser(), is("otherUser"));
        assertThat(properties.getPassword(), is("otherPassword"));
        assertThat(properties.getUrl(), is("https://localhost:8443"));
        assertThat(properties.isSslVerify(), is(false));
        assertThat(properties.getHttpProxy(), is(Optional.of("http://localhost:8080")));
        assertThat(properties.getConnectTimeout(), is(Duration.ofSeconds(120)));
        assertThat(properties.getReadTimeout(), is(Duration.ofSeconds(20)));
    }

    public static class TestConfiguration implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("spring.main.log-startup-info", "false"),
                    Map.entry("keycloak.ssl-verify", "false"),
                    Map.entry("keycloak.url", "https://localhost:8443"),
                    Map.entry("keycloak.login-realm", "moped"),
                    Map.entry("keycloak.client-id", "moped-client"),
                    Map.entry("keycloak.user", "otherUser"),
                    Map.entry("keycloak.password", "otherPassword"),
                    Map.entry("keycloak.http-proxy", "http://localhost:8080"),
                    Map.entry("keycloak.connect-timeout", "2m"),
                    Map.entry("keycloak.read-timeout", "20s")
            );
        }
    }
}
