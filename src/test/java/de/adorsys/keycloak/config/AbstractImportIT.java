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

import de.adorsys.keycloak.config.extensions.KeycloakExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

abstract public class AbstractImportIT extends AbstractImportTest {

    protected static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version");
    protected static final String KEYCLOAK_IMAGE = System.getProperty("keycloak.dockerImage", "quay.io/keycloak/keycloak");
    protected static final String KEYCLOAK_TAG_SUFFIX = System.getProperty("keycloak.dockerTagSuffix", "");

    public static final Network NETWORK = Network.newNetwork();

    @RegisterExtension
    public static KeycloakExtension keycloakExtension = new KeycloakExtension(NETWORK,
            DockerImageName.parse(KEYCLOAK_IMAGE + ":" + KEYCLOAK_VERSION + KEYCLOAK_TAG_SUFFIX));

}
