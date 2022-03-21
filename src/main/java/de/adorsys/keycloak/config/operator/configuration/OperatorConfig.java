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

package de.adorsys.keycloak.config.operator.configuration;

import de.adorsys.keycloak.config.operator.scope.RealmImportScope;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;

import static de.adorsys.keycloak.config.operator.scope.ImportScopeConfig.REALM_IMPORT;

@Configuration
@Profile("operator")
public class OperatorConfig {
    private static final Logger logger = LoggerFactory.getLogger(OperatorConfig.class);

    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }

    @Bean
    @Primary
    @Scope(value = REALM_IMPORT, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public KeycloakProvider keycloakProvider() {
        return new KeycloakProvider(RealmImportScope.getKeycloakProperties());
    }
}
