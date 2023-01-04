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

package com.github.xrodney.keycloak.operator.spec;

import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import io.fabric8.crd.generator.annotation.SchemaSwap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import javax.validation.constraints.NotNull;

@SchemaSwap(originalType = ComponentExportRepresentation.class, fieldName = "subComponents")
@SchemaSwap(originalType = GroupRepresentation.class, fieldName = "subGroups")
@SchemaSwap(originalType = ScopeRepresentation.class, fieldName = "policies")
@SchemaSwap(originalType = ScopeRepresentation.class, fieldName = "resources")
public class RealmSpec {
    @NotNull
    private RealmRepresentation realm;

    @NotNull
    private KeycloakConnection keycloakConnection;

    @NotNull
    private ImportConfigProperties importProperties;

    public RealmRepresentation getRealm() {
        return realm;
    }

    public void setRealm(RealmRepresentation realm) {
        this.realm = realm;
    }

    public KeycloakConnection getKeycloakConnection() {
        return keycloakConnection;
    }

    public void setKeycloakConnection(KeycloakConnection keycloakConnection) {
        this.keycloakConnection = keycloakConnection;
    }

    public ImportConfigProperties getImportProperties() {
        return importProperties;
    }

    public void setImportProperties(ImportConfigProperties importProperties) {
        this.importProperties = importProperties;
    }
}
