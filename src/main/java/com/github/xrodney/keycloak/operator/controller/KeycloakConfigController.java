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
import com.github.xrodney.keycloak.operator.spec.DefaultStatus;
import com.github.xrodney.keycloak.operator.spec.Realm;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.CloneUtil;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.control.ActivateRequestContext;

@ControllerConfiguration
//@GradualRetry()
public class KeycloakConfigController implements Reconciler<Realm>, Cleaner<Realm> {
    private static final Logger log = LoggerFactory.getLogger(KeycloakConfigController.class);
    private final CurrentRealmService currentRealmService;
    private final RealmImportService realmImportService;
    private final ReconciledResourceProvider reconciledResourceProvider;

    public KeycloakConfigController(CurrentRealmService currentRealmService,
                                    RealmImportService realmImportService, ReconciledResourceProvider reconciledResourceProvider) {
        this.currentRealmService = currentRealmService;
        this.realmImportService = realmImportService;
        this.reconciledResourceProvider = reconciledResourceProvider;
    }

    @Override
    @ActivateRequestContext
    public DeleteControl cleanup(Realm resource, Context context) {
        log.info("Execution cleanup for: {}", resource.getMetadata().getName());
        try {
            reconciledResourceProvider.setResource(resource);
            currentRealmService.runWithRealm(resource);

            String deployedRealm = resource.getStatus() != null ? resource.getStatus().getExternalId() : null;
            if (deployedRealm == null) {
                log.info("Not deployed? {}", resource.getMetadata().getName());
                return DeleteControl.defaultDelete();
            }

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
            currentRealmService.runWithRealm(resource);

            log.info("Execution createOrUpdateResource for: {}", resource.getMetadata().getName());

            RealmImport realmImport = CloneUtil.deepClone(resource.getSpec().getRealm(), RealmImport.class);

            String deployedRealm = resource.getStatus() != null && resource.getStatus().getExternalId() != null
                    ? resource.getStatus().getExternalId() : realmImport.getRealm();
            realmImportService.doImport(deployedRealm, realmImport);

            status.success(realmImport.getRealm());
            return UpdateControl.updateStatus(resource);
        } catch (Exception e) {
            log.error("Error while execute createOrUpdateResource", e);
            status.failure(e);
            return UpdateControl.updateStatus(resource);
        }
    }
}
