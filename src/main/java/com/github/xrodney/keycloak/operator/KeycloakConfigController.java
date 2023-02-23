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

package com.github.xrodney.keycloak.operator;

import com.github.xrodney.keycloak.operator.configuration.ReconciledResourceProvider;
import com.github.xrodney.keycloak.operator.spec.DefaultStatus;
import com.github.xrodney.keycloak.operator.spec.KeycloakConnection;
import com.github.xrodney.keycloak.operator.spec.Realm;
import com.github.xrodney.keycloak.operator.spec.SecretRef;
import de.adorsys.keycloak.config.configuration.ImportConfigPropertiesProvider;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImmutableKeycloakConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.CloneUtil;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.ws.rs.WebApplicationException;

@ControllerConfiguration
//@GradualRetry()
public class KeycloakConfigController implements Reconciler<Realm>, Cleaner<Realm> {
    private static final Logger log = LoggerFactory.getLogger(KeycloakConfigController.class);
    private final KubernetesClient kubernetesClient;
    private final KeycloakProvider keycloakProvider;
    private final ReconciledResourceProvider reconciledResourceProvider;
    private final ImportConfigPropertiesProvider importConfigPropertiesProvider;
    private final RealmImportService realmImportService;

    public KeycloakConfigController(KubernetesClient kubernetesClient,
                                    KeycloakProvider keycloakProvider,
                                    ReconciledResourceProvider reconciledResourceProvider,
                                    ImportConfigPropertiesProvider importConfigPropertiesProvider,
                                    RealmImportService realmImportService) {
        this.kubernetesClient = kubernetesClient;
        this.keycloakProvider = keycloakProvider;
        this.reconciledResourceProvider = reconciledResourceProvider;
        this.importConfigPropertiesProvider = importConfigPropertiesProvider;
        this.realmImportService = realmImportService;
    }

    @Override
    @ActivateRequestContext
    public DeleteControl cleanup(Realm resource, Context context) {
        log.info("Execution cleanup for: {}", resource.getMetadata().getName());
        reconciledResourceProvider.setResource(resource);

        String deployedRealm = resource.getStatus() != null ? resource.getStatus().getExternalId() : null;
        if (deployedRealm == null) {
            log.info("Not deployed? {}", resource.getMetadata().getName());
            return DeleteControl.defaultDelete();
        }

        try {
            importConfigPropertiesProvider.editConfig(config -> mergeConfig(resource.getSpec().getImportProperties(), config));
            keycloakProvider.editProperties(config -> mergeKeycloakConnection(resource, config));
            //AutoCloseable stateHandle = stateRepository.register(resource.getStatus());

            realmImportService.deleteRealm(deployedRealm);
            return DeleteControl.defaultDelete();

        } catch (Exception e) {
            log.error("Error while execute cleanup", e);
            return DeleteControl.noFinalizerRemoval();
        }
    }

    @Override
    @ActivateRequestContext
    public UpdateControl<Realm> reconcile(Realm resource, Context context) {
        DefaultStatus status = reconciledResourceProvider.setResourceWithStatus(resource, DefaultStatus::new);
        try {
            importConfigPropertiesProvider.editConfig(config -> mergeConfig(resource.getSpec().getImportProperties(), config));
            keycloakProvider.editProperties(config -> mergeKeycloakConnection(resource, config));

            log.info("Execution createOrUpdateResource for: {}", resource.getMetadata().getName());

            RealmImport realmImport = CloneUtil.deepClone(resource.getSpec().getRealm(), RealmImport.class);

            String deployedRealm = resource.getStatus() != null && resource.getStatus().getExternalId() != null
                    ? resource.getStatus().getExternalId() : realmImport.getRealm();
            realmImportService.doImport(deployedRealm, realmImport);

            status.setState(DefaultStatus.State.SUCCESS);
            status.setMessage("Successful import");
            status.setExternalId(realmImport.getRealm());
            resource.setStatus(status);

            return UpdateControl.updateStatus(resource);
        } catch (Exception e) {
            log.error("Error while execute createOrUpdateResource", e);
            resource.setStatus(errorStatus(status, e));

            return UpdateControl.updateStatus(resource);
        }
    }

    private ImportConfigProperties mergeConfig(ImportConfigProperties realmConfig, ImportConfigProperties globalConfig) {
        return globalConfig;
    }

    @NotNull
    private static DefaultStatus errorStatus(DefaultStatus status, Exception e) {
        status.setState(DefaultStatus.State.ERROR);
        if (e instanceof RuntimeException) {
            status.setException((RuntimeException) e);
        }

        String message = e.getMessage();
        if (e instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) e;
            try {
                if (wae.getResponse().hasEntity()) {
                    String entity = wae.getResponse().readEntity(String.class);
                    message += ": " + entity;
                }
            } catch (Exception ignore) {
                // no op
            }
        }
        status.setMessage(message);
        return status;
    }

    //@NotNull
    private KeycloakConfigProperties mergeKeycloakConnection(Realm resource, KeycloakConfigProperties config) {
        KeycloakConnection realmConfig = resource.getSpec().getKeycloakConnection();
        String currentNamespace = resource.getMetadata().getNamespace();

        var builder = ImmutableKeycloakConfigProperties.builder().from(config);
        if (realmConfig.getLoginRealm() != null) {
            builder.loginRealm(realmConfig.getLoginRealm());
        }
        if (realmConfig.getClientId() != null) {
            builder.clientId(realmConfig.getClientId());
        }
        if (realmConfig.getUrl() != null) {
            builder.url(realmConfig.getUrl());
        }

        if (realmConfig.getUser() != null) {
            builder.user(realmConfig.getUser());
        }

        if (realmConfig.getPasswordSecret() != null) {
            builder.password(readPassword(realmConfig.getPasswordSecret(), currentNamespace));
        }

        if (realmConfig.getClientSecretSecret() != null) {
            builder.clientSecret(readPassword(realmConfig.getClientSecretSecret(), currentNamespace));
        }

        if (realmConfig.getGrantType() != null) {
            builder.grantType(realmConfig.getGrantType());
        }
        return builder.build();
    }

    private String readPassword(SecretRef secretRef, String currentNamespace) {
        if (StringUtils.isNotEmpty(secretRef.getImmediateValue())) {
            return secretRef.getImmediateValue();
        }

        Secret credentialSecret = kubernetesClient
                .secrets()
                .inNamespace(Objects.requireNonNullElse(secretRef.getNamespace(), currentNamespace))
                .withName(secretRef.getName())
                .get();

        if (credentialSecret == null) {
            throw new IllegalStateException("The linked credential secret does not exist");
        }

        log.info("Successfully read the credential secret");
        String password = credentialSecret.getData().get(secretRef.getKey());

        if (password == null) {
            throw new IllegalStateException("The linked credential secret does not contain password");
        }
        password = new String(Base64.getDecoder().decode(password));
        log.info("Successfully read password from the credential secret");

        return password;
    }
}
