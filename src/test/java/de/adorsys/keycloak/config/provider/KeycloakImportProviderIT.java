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
import de.adorsys.keycloak.config.properties.ImmutableImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImmutableImportFilesProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"java:S5976"})
@QuarkusTest
class KeycloakImportProviderIT extends AbstractImportTest {
    private static String BASE = KeycloakImportProviderIT.class.getClassLoader().getResource("").toString();

    @BeforeEach
    void setUp() {
        configPropertiesProvider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .files(ImmutableImportFilesProperties.builder().from(config.getFiles())
                        .isIncludeHiddenFiles(false)
                        .build())
                .build()
        );
    }

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
    void shouldReadLocalFilesFromDirectorySorted() {
        String location = BASE + "/import-files/import/sorted";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/0_create_realm\\.json"),
                matchesPattern(".+/1_update_realm\\.json"),
                matchesPattern(".+/2_update_realm\\.json"),
                matchesPattern(".+/4_update_realm\\.json"),
                matchesPattern(".+/5_update_realm\\.json"),
                matchesPattern(".+/6_update_realm\\.json"),
                matchesPattern(".+/7_update_realm\\.json"),
                matchesPattern(".+/9_update_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromWildcardPatternWithDirectoryTraversal() {
        String location = "file:src/test/resources/import-files/import/../import/wildcard";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);
        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location), hasKey(
                matchesPattern(".+/0_create_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromManyDirectories() {
        String location1 = BASE + "/import-files/import/wildcard/sub";
        String location2 = BASE + "/import-files/import/wildcard/another";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location1, location2);
        assertThat(keycloakImport.getRealmImports().keySet(), contains(is(location1), is(location2)));
        assertThat(keycloakImport.getRealmImports().get(location1), allOf(
                hasKey(matchesPattern(".+/sub/directory/4_update_realm\\.json")),
                hasKey(matchesPattern(".+/sub/directory/5_update_realm\\.json")),
                hasKey(matchesPattern(".+/sub/directory/6_update_realm\\.json")),
                hasKey(matchesPattern(".+/sub/directory/7_update_realm\\.yaml"))
        ));
        assertThat(keycloakImport.getRealmImports().get(location2), allOf(
                hasKey(matchesPattern(".+/another/directory/1_update_realm\\.json")),
                hasKey(matchesPattern(".+/another/directory/2_update_realm\\.json")),
                hasKey(matchesPattern(".+/another/directory/3_update_realm\\.json"))
        ));
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


    @Test
    void shouldReadLocalFilesFromDirectorySortedIncludingHiddenFiles() {
        configPropertiesProvider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .files(ImmutableImportFilesProperties.builder().from(config.getFiles())
                        .isIncludeHiddenFiles(true)
                        .build()
                )
                .build()
        );

        String location = BASE + "/import-files/import/sorted";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), containsInAnyOrder(
                matchesPattern(".+/.3_update_realm\\.json"),
                matchesPattern(".+/.8_update_realm\\.json"),
                matchesPattern(".+/0_create_realm\\.json"),
                matchesPattern(".+/1_update_realm\\.json"),
                matchesPattern(".+/2_update_realm\\.json"),
                matchesPattern(".+/4_update_realm\\.json"),
                matchesPattern(".+/5_update_realm\\.json"),
                matchesPattern(".+/6_update_realm\\.json"),
                matchesPattern(".+/7_update_realm\\.json"),
                matchesPattern(".+/9_update_realm\\.json")
        ));
    }

    @Test
    void shouldReadLocalFilesFromDirectorySortedExcludingPatterns() {
        configPropertiesProvider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .files(ImmutableImportFilesProperties.builder().from(config.getFiles())
                        .excludes(List.of("*create*", "4_*"))
                        .build()
                )
                .build()
        );

        String location = BASE + "/import-files/import/sorted";
        KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(location);

        assertThat(keycloakImport.getRealmImports(), hasKey(is(location)));
        assertThat(keycloakImport.getRealmImports().get(location).keySet(), contains(
                matchesPattern(".+/1_update_realm\\.json"),
                matchesPattern(".+/2_update_realm\\.json"),
                matchesPattern(".+/5_update_realm\\.json"),
                matchesPattern(".+/6_update_realm\\.json"),
                matchesPattern(".+/7_update_realm\\.json"),
                matchesPattern(".+/9_update_realm\\.json")
        ));
    }
}
