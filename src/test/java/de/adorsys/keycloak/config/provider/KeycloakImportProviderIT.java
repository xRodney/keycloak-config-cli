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

import de.adorsys.keycloak.config.AbstractImportTest;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.KeycloakImport;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5976"})
@QuarkusTest
class KeycloakImportProviderIT extends AbstractImportTest {
    private static String BASE = KeycloakImportProviderIT.class.getClassLoader().getResource("").toString();

    @Test
    void shouldReadLocalFile() {
        String location = BASE + "/import-files/import/single/0_create_realm.json";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json$")
        ));
    }

    @Test
    void shouldReadLocalFileLegacy() throws IOException {
        Path realmFile = Files.createTempFile("realm", ".json");
        Path tempFilePath = Files.writeString(realmFile, "{\"enabled\": true, \"realm\": \"realm-sorted-import\"}");

        String importPath = "file:/" + tempFilePath.toAbsolutePath().toString().replace("\\", "/");
        KeycloakImport keycloakImport = keycloakImportProvider
                .readFromLocations(importPath);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(importPath)));
        assertThat(keycloakImport.getRealmImports().get(importPath).keySet(), contains(importPath));
    }

    @Test
    void shouldFailOnDirectory() {
        String location = BASE + "/import-files/import/sorted";
        InvalidImportException exception = assertThrows(InvalidImportException.class, () -> keycloakImportProvider.readFromLocations(location));

        assertThat(exception.getMessage(), is("Unable to load "+location+". It is a directory"));
    }

    @Test
    void shouldFailOnInvalidClassPath() {
        InvalidImportException exception = assertThrows(InvalidImportException.class, () -> keycloakImportProvider.readFromLocations("classpath:/invalid"));

        assertThat(exception.getMessage(), is("No files matching 'classpath:/invalid'!"));
    }

    @Test
    void shouldFailOnInvalidFile() {
        InvalidImportException exception = assertThrows(InvalidImportException.class, () -> keycloakImportProvider.readFromLocations("invalid"));

        assertThat(exception.getMessage(), is("No files matching 'invalid'!"));
    }
}
