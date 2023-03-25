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

import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class KeycloakExtension implements TestWatcher, BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {

    public final ToStringConsumer logsConsumer = new ToStringConsumer();
    protected static final String KEYCLOAK_LOG_LEVEL = System.getProperty("keycloak.loglevel", "DEBUG");

    private final GenericContainer<?> container;
    private static boolean outputLog = false;
    private static boolean started = false;
    private final DockerImageName imageName;

    public KeycloakExtension(Network network, DockerImageName imageName) {
        this.imageName = imageName;
        this.container = new GenericContainer<>(imageName)
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_USER", "admin")
                .withEnv("KEYCLOAK_PASSWORD", "admin123")
                .withEnv("KEYCLOAK_LOGLEVEL", KEYCLOAK_LOG_LEVEL)
                .withEnv("ROOT_LOGLEVEL", "ERROR")
                // keycloak-x
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin123")
                .withEnv("QUARKUS_PROFILE", "dev")
                .withExtraHost("host.docker.internal", "host-gateway")
                .waitingFor(Wait.forHttp("/"))
                .withNetwork(network)
                .withStartupTimeout(Duration.ofSeconds(300));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        outputLog = true;
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        outputLog = true;
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        if (started) {
            return;
        }
        started = true;

        boolean isLegacyDistribution = imageName.asCanonicalNameString().contains("legacy")
                || (VersionUtil.lt(imageName.getVersionPart(), "17") && !imageName.asCanonicalNameString().contains("keycloak-x"));

        List<String> command = new ArrayList<>();

        if (isLegacyDistribution) {
            command.add("-c");
            command.add("standalone.xml");
            command.add("-Dkeycloak.profile.feature.admin_fine_grained_authz=enabled");
            command.add("-Dkeycloak.profile.feature.declarative_user_profile=enabled");
        } else {
            container.setCommand("start-dev");
            command.add("start-dev");
            command.add("--features");
            command.add("admin-fine-grained-authz,declarative-user-profile");
            command.add("--log-level=" + KEYCLOAK_LOG_LEVEL);
        }

        if (System.getProperties().getOrDefault("skipContainerStart", "false").equals("false")) {
            container.setCommand(command.toArray(new String[0]));
            container.start();
            container.followOutput(logsConsumer);

            // container.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger("\uD83D\uDC33 [" + container.getDockerImageName() + "]")));
            System.setProperty("keycloak.user", container.getEnvMap().get("KEYCLOAK_USER"));
            System.setProperty("keycloak.password", container.getEnvMap().get("KEYCLOAK_PASSWORD"));
            var baseUrl = String.format(
                    "http://%s:%d", container.getContainerIpAddress(), container.getMappedPort(8080)
            );
            //System.setProperty("keycloak.baseUrl", baseUrl);

            if (isLegacyDistribution) {
                System.setProperty("keycloak.url", baseUrl + "/auth/");
            } else {
                System.setProperty("keycloak.url", baseUrl);
            }
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) {
    }

    @Override
    public void close() {
        synchronized (KeycloakExtension.class) {
            if (started) {
                container.stop();
                started = false;
            }
            if (outputLog) {
                outputLog = false;

                String logs = logsConsumer.toUtf8String();
                System.out.println("\n\nKeycloak Container Logs:");
                // Remove double newlines
                // See: https://github.com/testcontainers/testcontainers-java/issues/1763
                System.out.println(logs.replaceAll("\n\n", "\n"));
            }
        }
    }
}
