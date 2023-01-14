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

import de.adorsys.keycloak.config.configuration.ImportConfigPropertiesProvider;
import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.provider.KeycloakImportProvider;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.test.util.KeycloakAuthentication;
import de.adorsys.keycloak.config.test.util.KeycloakRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

@ExtendWith(GithubActionsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Timeout(value = 30, unit = SECONDS)
abstract public class AbstractImportTest {
    @Autowired
    public RealmImportService realmImportService;

    @Autowired
    public KeycloakImportProvider keycloakImportProvider;

    @Autowired
    public KeycloakProvider keycloakProvider;

    @Autowired
    public KeycloakRepository keycloakRepository;

    @Autowired
    public KeycloakAuthentication keycloakAuthentication;

    @Autowired
    public ImportConfigPropertiesProvider configPropertiesProvider;

    public String resourcePath;

    public void doImport(String fileName) throws IOException {
        List<RealmImport> realmImports = getImport(fileName);

        for (RealmImport realmImport : realmImports) {
            realmImportService.doImport(realmImport);
        }
    }

    public RealmImport getFirstImport(String fileName) throws IOException {
        return getImport(fileName).get(0);
    }

    public List<RealmImport> getImport(String fileName) throws IOException {
        URL url = getClass().getClassLoader().getResource(this.resourcePath + '/' + fileName);
        if (url == null) {
            throw new IllegalArgumentException(fileName);
        }
        String location = url.toString();



        return keycloakImportProvider
                .readFromLocations(location)
                .getRealmImports()
                .get(location)
                .entrySet()
                .stream()
                .findFirst()
                .orElseThrow()
                .getValue();
    }
}
