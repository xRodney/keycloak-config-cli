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

package com.github.xrodney.keycloak.operator.controller;

import com.github.xrodney.keycloak.operator.configuration.ReconciledResourceProvider;
import com.github.xrodney.keycloak.operator.service.CurrentRealmService;
import com.github.xrodney.keycloak.operator.service.SecretsManager;
import com.github.xrodney.keycloak.operator.spec.Client;
import com.github.xrodney.keycloak.operator.spec.DefaultStatus;
import de.adorsys.keycloak.config.service.ClientImportService;
import de.adorsys.keycloak.config.util.CloneUtil;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.control.ActivateRequestContext;

@ControllerConfiguration
//@GradualRetry()
public class ClientController implements Reconciler<Client>, Cleaner<Client> {
    private static final Logger log = LoggerFactory.getLogger(ClientController.class);
    private final CurrentRealmService currentRealmService;
    private final ClientImportService clientImportService;
    private final ReconciledResourceProvider reconciledResourceProvider;
    private final SecretsManager secretsManager;

    public ClientController(CurrentRealmService currentRealmService,
                            ClientImportService clientImportService, ReconciledResourceProvider reconciledResourceProvider, SecretsManager secretsManager) {
        this.currentRealmService = currentRealmService;
        this.clientImportService = clientImportService;
        this.reconciledResourceProvider = reconciledResourceProvider;
        this.secretsManager = secretsManager;
    }

    @Override
    @ActivateRequestContext
    public DeleteControl cleanup(Client resource, Context context) {
        log.info("Execution cleanup for: {}", resource.getMetadata().getName());
        reconciledResourceProvider.setResource(resource);

        return currentRealmService.runWithRealm(resource, realm -> {
            String deployedId = resource.getStatus() != null ? resource.getStatus().getExternalId() : null;
            if (deployedId == null) {
                log.info("Not deployed? {}", resource.getMetadata().getName());
                return DeleteControl.defaultDelete();
            }

            var client = resource.getSpec().getClient();
            client.setId(deployedId);

            try {
                clientImportService.delete(realm.getRealm(), resource.getSpec().getClient());
                return DeleteControl.defaultDelete();

            } catch (Exception e) {
                log.error("Error while execute cleanup", e);
                return DeleteControl.noFinalizerRemoval();
            }
        });
    }

    @Override
    @ActivateRequestContext
    public UpdateControl<Client> reconcile(Client resource, Context context) {
        DefaultStatus status = reconciledResourceProvider.setResourceWithStatus(resource, DefaultStatus::new);
        var spec = resource.getSpec();
        var metadata = resource.getMetadata();

        return currentRealmService.runWithRealm(resource, realm -> {
            try {
                log.info("Execution createOrUpdateResource for: {}", metadata.getName());

                ClientRepresentation client = CloneUtil.deepClone(spec.getClient(), ClientRepresentation.class);
                if (spec.getClientSecretRef() != null) {
                    client.setSecret(secretsManager.readPassword(spec.getClientSecretRef()));
                }
                client.setId(status.getExternalId());
                clientImportService.doImport(realm.getRealm(), client);

                status.success(client.getId());
                return UpdateControl.updateStatus(resource);
            } catch (Exception e) {
                log.error("Error while execute createOrUpdateResource", e);
                status.failure(e);
                return UpdateControl.updateStatus(resource);
            }
        });
    }
}
