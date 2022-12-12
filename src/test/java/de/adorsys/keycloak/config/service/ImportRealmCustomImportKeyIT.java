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
import de.adorsys.keycloak.config.properties.ImmutableImportCacheProperties;
import de.adorsys.keycloak.config.properties.ImmutableImportConfigProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

@QuarkusTest
class ImportRealmCustomImportKeyIT extends AbstractImportIT {
    private static final String REALM_NAME = "realm-custom-import-key";

    ImportRealmCustomImportKeyIT() {
        this.resourcePath = "import-files/realm-custom-import-key";
    }

    @BeforeEach
    void setUp() {
        configPropertiesProvider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .cache(ImmutableImportCacheProperties.builder().from(config.getCache())
                        .key("custom")
                        .build()
                )
                .build());
    }

    @Test
    @Order(0)
    void shouldCreateSimpleRealm() throws IOException {
        doImport("00_create_simple-realm.json");

        RealmRepresentation createdRealm = keycloakProvider.getInstance().realm(REALM_NAME).toRepresentation();

        assertThat(createdRealm.getRealm(), is(REALM_NAME));
        assertThat(createdRealm.isEnabled(), is(true));
        assertThat(createdRealm.getLoginTheme(), is(nullValue()));
    }
}
