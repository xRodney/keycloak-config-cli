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

package de.adorsys.keycloak.config.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.Optional;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ConfigMapping(prefix = "keycloak")
@SuppressWarnings({"java:S107"})
@Value.Immutable
public interface KeycloakConfigProperties {

    @NotBlank
    @WithName("login-realm")
    String getLoginRealm();

    @NotBlank
    @WithName("client-id")
    String getClientId();

    @NotNull
    @WithName("version")
    String getVersion();

    @NotNull
    @ConfigProperty(defaultValue = "localhost")
    @WithName("url")
    String getUrl();

    @WithName("user")
    String getUser();

    @WithName("password")
    String getPassword();

    @WithName("client-secret")
    String getClientSecret();

    @WithName("grant-type")
    @NotBlank
    String getGrantType();

    @WithName("ssl-verify")
    @NotNull
    boolean isSslVerify();

    @WithName("http-proxy")
    Optional<String> getHttpProxy();

    @WithName("connect-timeout")
    Duration getConnectTimeout();

    @WithName("read-timeout")
    Duration getReadTimeout();
}
