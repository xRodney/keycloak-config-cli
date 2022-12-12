/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.configuration;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import io.quarkus.arc.Priority;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.SmallRyeConfig;

import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class ConfigProducer {

    @Produces
    @RequestScoped
    @Alternative
    @Priority(1)
    KeycloakConfigProperties keycloakConfigProperties(SmallRyeConfig config) {
        ConfigMappings.registerConfigMappings(config, Set.of(new ConfigMappings.ConfigClassWithPrefix(KeycloakConfigProperties.class, "keycloak")));
        return config.getConfigMapping(KeycloakConfigProperties.class);
    }
}
