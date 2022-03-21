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

package de.adorsys.keycloak.config.configuration;

import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.FileComparator;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.util.Comparator;

@Configuration
public class KeycloakConfigConfiguration {
    private final ResourceLoader resourceLoader;

    @Autowired
    public KeycloakConfigConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public PathMatchingResourcePatternResolver patternResolver() {
        return new PathMatchingResourcePatternResolver(this.resourceLoader);
    }

    @Bean
    public Comparator<File> fileComparator() {
        return new FileComparator();
    }

    @Bean
    @Profile("!operator")
    public KeycloakProvider keycloakProvider(KeycloakConfigProperties properties) {
        return new KeycloakProvider(properties);
    }
}
