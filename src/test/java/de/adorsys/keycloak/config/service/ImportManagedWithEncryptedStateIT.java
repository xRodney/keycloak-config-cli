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
import de.adorsys.keycloak.config.properties.ImmutableImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImmutableImportManagedProperties;
import de.adorsys.keycloak.config.properties.ImmutableImportRemoteStateProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.MessageFormat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@QuarkusTest
class ImportManagedWithEncryptedStateIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithManagedEncryptedSate";

    @Autowired
    public ImportConfigProperties importConfigProperties;

    ImportManagedWithEncryptedStateIT() {
        this.resourcePath = "import-files/managed-encrypted-state";
    }


    @BeforeEach
    void setUp() {
        configPropertiesProvider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .managed(ImmutableImportManagedProperties.builder().from(config.getManaged())
                        .role(ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues.FULL)
                        .build()
                )
                .remoteState(ImmutableImportRemoteStateProperties.builder().from(config.getRemoteState())
                        .encryptionKey("password")
                        .build()
                )
                .build());
    }

    @Test
    @Order(0)
    void shouldCreateEncryptedState() throws IOException {
        doImport("0_create_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        String attributeKey = MessageFormat.format(
                ImportConfigProperties.REALM_STATE_ATTRIBUTE_PREFIX_KEY,
                "roles-realm"
        ) + "-0";

        assertThat(realm.getAttributes().get(attributeKey), not(containsString("role")));
    }

    @Test
    @Order(1)
    void shouldUpdateEncryptedState() throws IOException {
        doImport("1_update_realm.json");

        RealmRepresentation realm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();
        assertThat(realm.getRealm(), is(REALM_NAME));
        assertThat(realm.isEnabled(), is(true));

        String attributeKey = MessageFormat.format(
                ImportConfigProperties.REALM_STATE_ATTRIBUTE_PREFIX_KEY,
                "roles-realm"
        ) + "-0";

        assertThat(realm.getAttributes().get(attributeKey), not(containsString("role")));
    }
}
