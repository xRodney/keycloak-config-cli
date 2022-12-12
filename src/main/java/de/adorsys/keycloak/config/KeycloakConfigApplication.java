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

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

//@SpringBootApplication(proxyBeanMethods = false)
//@EnableConfigurationProperties({KeycloakConfigProperties.class, ImportConfigProperties.class})
@QuarkusMain
public class KeycloakConfigApplication {
    public static void main(String[] args) {
        Quarkus.run(KeycloakConfigRunner.class, args);
        // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-application-exit
        //        System.exit(
        //                SpringApplication.exit(SpringApplication.run(KeycloakConfigApplication.class, args))
        //        );
    }
}
