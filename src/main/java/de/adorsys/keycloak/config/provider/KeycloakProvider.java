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

package de.adorsys.keycloak.config.provider;

import de.adorsys.keycloak.config.exception.KeycloakProviderException;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.util.ResteasyUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.UnaryOperator;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

/**
 * This class exists because we need to create a single keycloak instance or to close the keycloak before using a new one
 * to avoid a deadlock.
 */
@RequestScoped
public class KeycloakProvider implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakProvider.class);

    private KeycloakConfigProperties properties;
    private ResteasyClient resteasyClient;

    private Keycloak keycloak;

    private String version;

    @Inject
    public KeycloakProvider(KeycloakConfigProperties properties) {
        this.properties = properties;
    }

    public void setProperties(KeycloakConfigProperties properties) {
        close();
        this.properties = properties;
    }

    public void editProperties(UnaryOperator<KeycloakConfigProperties> editor) {
        setProperties(editor.apply(properties));
    }

    public KeycloakConfigProperties getProperties() {
        return properties;
    }

    public Keycloak getInstance() {
        if (keycloak == null || keycloak.isClosed()) {
            keycloak = createKeycloak();

            checkServerVersion();
        }

        return keycloak;
    }

    public String getKeycloakVersion() {
        if (version == null) {
            version = getInstance().serverInfo().getInfo().getSystemInfo().getVersion();
        }

        return version;
    }

    public void refreshToken() {
        getInstance().tokenManager().refreshToken();
    }

    public <T> T getCustomApiProxy(Class<T> proxyClass) {
        try {
            URI uri = new URI(properties.getUrl());
            return getInstance().proxy(proxyClass, uri);
        } catch (URISyntaxException e) {
            throw new KeycloakProviderException(e);
        }
    }

    private Keycloak createKeycloak() {
        Keycloak result;
        result = getKeycloak();

        return result;
    }

    private Keycloak getKeycloak() {
        String serverUrl = properties.getUrl();

        Keycloak keycloakInstance = getKeycloakInstance(serverUrl);
        keycloakInstance.tokenManager().getAccessToken();

        return keycloakInstance;
    }

    private Keycloak getKeycloakInstance(String serverUrl) {
        this.resteasyClient = ResteasyUtil.getClient(
                !this.properties.isSslVerify(),
                this.properties.getHttpProxy(),
                this.properties.getConnectTimeout(),
                this.properties.getReadTimeout()
        );

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(properties.getLoginRealm())
                .clientId(properties.getClientId())
                .grantType(properties.getGrantType())
                .clientSecret(properties.getClientSecret())
                .username(properties.getUser())
                .password(properties.getPassword())
                .resteasyClient(resteasyClient)
                .build();
    }

    private void checkServerVersion() {
        if (properties.getVersion().equals("@keycloak.version@")) return;

        String kccKeycloakMajorVersion = properties.getVersion().split("\\.")[0];

        if (!getKeycloakVersion().startsWith(kccKeycloakMajorVersion)) {
            logger.warn(
                    "Local keycloak-config-cli ({}-{}) and remote Keycloak ({}) may not compatible.",
                    getClass().getPackage().getImplementationVersion(),
                    properties.getVersion(),
                    getKeycloakVersion()
            );
        }
    }

    @Override
    @PreDestroy
    public void close() {
        if (!isClosed()) {
            logout();
            keycloak.close();
        }
    }

    // see: https://github.com/keycloak/keycloak/blob/8ea09d38168c22937363cf77a07f9de5dc7b48b0/services/src/main/java/org/keycloak/protocol/oidc/endpoints/LogoutEndpoint.java#L207-L220

    /**
     * Logout a session via a non-browser invocation.  Similar signature to refresh token except there is no grant_type.
     * You must pass in the refresh token and
     * authenticate the client if it is not public.
     * <p>
     * If the client is a confidential client
     * you must include the client-id and secret in a Basic Auth Authorization header.
     * <p>
     * If the client is a public client, then you must include a "client_id" form parameter.
     * <p>
     * returns 204 if successful, 400 if not with a json error response.
     */
    private void logout() {
        String refreshToken = this.keycloak.tokenManager().getAccessToken().getRefreshToken();
        // if we do not have a refreshToken, we are not able ot logout (grant_type=client_credentials)
        if (refreshToken == null) {
            return;
        }

        ResteasyWebTarget resteasyWebTarget = resteasyClient
                .target(properties.getUrl().toString())
                .path("/realms/" + properties.getLoginRealm() + "/protocol/openid-connect/logout");

        Form form = new Form();
        form.param("refresh_token", refreshToken);

        if (!properties.getClientId().isEmpty() && properties.getClientSecret().isEmpty()) {
            form.param("client_id", properties.getClientId());
        }

        if (!properties.getClientId().isEmpty() && !properties.getClientSecret().isEmpty()) {
            resteasyWebTarget.register(new BasicAuthentication(properties.getClientId(), properties.getClientSecret()));
        }

        Response response = resteasyWebTarget.request().post(Entity.form(form));
        // if debugging is enabled, care about error on logout.
        if (!response.getStatusInfo().equals(Response.Status.NO_CONTENT)) {
            logger.warn("Unable to logout. HTTP Status: {}", response.getStatus());
            if (logger.isDebugEnabled()) {
                throw new WebApplicationException(response);
            }
        }
    }

    public boolean isClosed() {
        return keycloak == null || keycloak.isClosed();
    }
}
