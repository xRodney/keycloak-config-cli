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

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.properties.ImmutableKeycloakConfigProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;
import javax.ws.rs.NotAuthorizedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@QuarkusTest
class AuthorizeImportUsingServiceAccountIT extends AbstractImportIT {
    private static final String MASTER_REALM = "master";
    private static final String SERVICE_ACCOUNT_REALM = "service-account";

    AuthorizeImportUsingServiceAccountIT() {
        this.resourcePath = "import-files/service-account";
    }

    @Test
    @Order(0)
    void createServiceAccountInMasterRealm() throws IOException {
        doImport("00_update_realm_create_service_account_in_master_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(MASTER_REALM).toRepresentation();
        assertThat(realm.getRealm(), is(MASTER_REALM));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(MASTER_REALM, "config-cli-master");

        assertThat(client.isServiceAccountsEnabled(), is(true));
    }

    @Test
    @Order(1)
    void createNewRealm() throws IOException {
        keycloakProvider.editProperties(properties -> ImmutableKeycloakConfigProperties.builder().from(properties)
                .loginRealm(MASTER_REALM)
                .grantType("client_credentials")
                .clientId("config-cli-master")
                .clientSecret("config-cli-master-secret")
                .build()
        );

        doImport("01_create_realm_client_with_service_account_enabled.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(SERVICE_ACCOUNT_REALM).toRepresentation();

        assertThat(realm.getRealm(), is(SERVICE_ACCOUNT_REALM));
        assertThat(realm.isEnabled(), is(true));

        ClientRepresentation client = keycloakRepository.getClient(SERVICE_ACCOUNT_REALM, "config-cli");

        assertThat(client.isServiceAccountsEnabled(), is(true));

        Assertions.assertDoesNotThrow(() -> keycloakProvider.close());
    }

    @Test
    @Order(2)
    void masterCredentialsUnauthorized() throws IOException {
        keycloakProvider.editProperties(properties -> ImmutableKeycloakConfigProperties.builder().from(properties)
                .loginRealm(MASTER_REALM)
                .grantType("client_credentials")
                .clientId("bogus-client-id")
                .clientSecret("bogus-client-secret")
                .build()
        );

        assertImportFails(NotAuthorizedException.class,
                "01_create_realm_client_with_service_account_enabled.json");

        Assertions.assertDoesNotThrow(() -> keycloakProvider.close());
    }

    @Test
    @Order(3)
    void updateExistingRealm() throws IOException {
        keycloakProvider.editProperties(properties -> ImmutableKeycloakConfigProperties.builder().from(properties)
                .loginRealm("service-account")
                .grantType("client_credentials")
                .clientId("config-cli")
                .clientSecret("config-cli-secret")
                .build()
        );

        doImport("02_update_realm_client_with_service_account_enabled.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(SERVICE_ACCOUNT_REALM).toRepresentation();

        assertThat(realm.getRealm(), is(SERVICE_ACCOUNT_REALM));
        assertThat(realm.isEnabled(), is(true));
        assertThat(realm.getLoginTheme(), is("moped"));

        Assertions.assertDoesNotThrow(() -> keycloakProvider.close());
    }


    @Test
    @Order(3)
    void serviceAccountRealmCredentialsUnauthorized() throws IOException {
        keycloakProvider.editProperties(properties -> ImmutableKeycloakConfigProperties.builder().from(properties)
                .loginRealm("service-account")
                .grantType("client_credentials")
                .clientId("config-cli")
                .clientSecret("bogus")
                .build()
        );

        assertImportFails(NotAuthorizedException.class,
                "02_update_realm_client_with_service_account_enabled.json");


        Assertions.assertDoesNotThrow(() -> keycloakProvider.close());
    }
}
