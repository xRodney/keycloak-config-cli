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

import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.provider.KeycloakImportProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import io.quarkus.runtime.QuarkusApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

@Singleton
public class KeycloakConfigRunner implements QuarkusApplication {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigRunner.class);
    private static final long START_TIME = System.currentTimeMillis();

    private final KeycloakImportProvider keycloakImportProvider;
    private final RealmImportService realmImportService;

    @Autowired
    public KeycloakConfigRunner(
            KeycloakImportProvider keycloakImportProvider,
            RealmImportService realmImportService) {
        this.keycloakImportProvider = keycloakImportProvider;
        this.realmImportService = realmImportService;
    }

    @Override
    public int run(String... args) {
        int exitCode = 0;
        try {
            String[] locations = args.length == 0 ? new String[] {"."} : args;
            KeycloakImport keycloakImport = keycloakImportProvider.readFromLocations(locations);

            Map<String, Map<String, List<RealmImport>>> realmImports = keycloakImport.getRealmImports();

            for (Map<String, List<RealmImport>> realmImportLocations : realmImports.values()) {
                for (Map.Entry<String, List<RealmImport>> realmImport : realmImportLocations.entrySet()) {
                    logger.info("Importing file '{}'", realmImport.getKey());
                    for (RealmImport realmImportParts : realmImport.getValue()) {
                        realmImportService.doImport(realmImportParts);
                    }
                }
            }
        } catch (NullPointerException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());

            exitCode = 1;

            if (logger.isDebugEnabled()) {
                throw e;
            }
        } finally {
            long totalTime = System.currentTimeMillis() - START_TIME;
            String formattedTime = new SimpleDateFormat("mm:ss.SSS").format(new Date(totalTime));
            logger.info("keycloak-config-cli running in {}.", formattedTime);
        }

        return exitCode;
    }
}
