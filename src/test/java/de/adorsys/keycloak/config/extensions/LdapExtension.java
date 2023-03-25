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

package de.adorsys.keycloak.config.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public class LdapExtension implements BeforeAllCallback, AfterAllCallback {
    public static int LDAP_PORT = 389;
    public static String LDAP_HOST = "ldap.internal";

    private static final Logger LOG = LoggerFactory.getLogger(LdapExtension.class);
    public final String ldif;
    public final String baseDN;
    public final String bindDN;
    public final String password;

    private GenericContainer<?> container;

    public LdapExtension(String baseDN, String ldif, String bindDN, String password, Network network) {
        this.ldif = ldif;
        this.baseDN = baseDN;
        this.bindDN = bindDN;
        this.password = password;

        container = new GenericContainer<>(new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "embedded-ldap.Dockerfile"))
                .withCopyFileToContainer(MountableFile.forClasspathResource(ldif), "/etc/ldap.ldif")
                .withEnv("LDAP_LDIF_FILE", "/etc/ldap.ldif")
                .withEnv("baseDN", baseDN)
                .withEnv("port", String.valueOf(LDAP_PORT))
                .withEnv("additionalBindDN", bindDN)
                .withEnv("additionalBindPassword", password)
                .withNetworkAliases(LDAP_HOST)
                .withNetwork(network)
                .withExposedPorts(LDAP_PORT);
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        LOG.info("LDAP server starting...");
        container.start();
        LOG.info("LDAP server started. Listen on port " + LDAP_PORT + ", external localhost:"
                + container.getFirstMappedPort());
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        container.stop();
    }
}
