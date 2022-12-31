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

package de.adorsys.keycloak.config;

import de.adorsys.keycloak.config.exception.InvalidImportException;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class CommandLineImportIT extends AbstractImportIT {
    @Inject
    private KeycloakConfigRunner runner;

    @Test
    void testImportFile() {
        var exitCode = runner.run(
                "src/test/resources/import-files/cli/file.json"
        );
        assertThat(exitCode, is(0));

        RealmRepresentation fileRealm = keycloakProvider.getInstance().realm("file").toRepresentation();

        assertThat(fileRealm.getRealm(), is("file"));
        assertThat(fileRealm.isEnabled(), is(true));
    }

    @Test
    void testImportDirectory() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, () ->
                runner.run("src/test/resources/import-files/cli/dir"));

        assertThat(thrown.getMessage(), is("Unable to load src/test/resources/import-files/cli/dir. It is a directory"));
    }

    @Test
    void testInvalidFileFormatException() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, () ->
                runner.run("src/test/resources/application-IT.properties"));

        assertThat(thrown.getMessage(), stringContainsInOrder("Unable to parse file", "application-IT.properties", "Cannot construct instance of `de.adorsys.keycloak.config.model.RealmImport`"));
    }

    @Test
    void testInvalidImportException() {
        InvalidImportException thrown = assertThrows(InvalidImportException.class, () ->
                runner.run("invalid"));

        assertThat(thrown.getMessage(), Matchers.startsWith("No files matching 'invalid'!"));
    }
}
