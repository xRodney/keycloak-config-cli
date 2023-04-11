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
import com.github.xrodney.keycloak.operator.spec.SecretRef;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.service.RealmImportService;
import de.adorsys.keycloak.config.util.CloneUtil;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.control.ActivateRequestContext;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ControllerConfiguration
//@GradualRetry()
public class KeycloakConfigController implements Reconciler<Realm>, Cleaner<Realm>, EventSourceInitializer<Realm> {
    private static final Logger log = LoggerFactory.getLogger(KeycloakConfigController.class);
    private final CurrentRealmService currentRealmService;
    private final RealmImportService realmImportService;
    private final ReconciledResourceProvider reconciledResourceProvider;

    private final DependentResourcesMapper<Realm, Secret> dependentSecrets
            = new DependentResourcesMapper<>(this::dependsOnSecrets);

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
        dependentSecrets.onDelete(resource);
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
        log.info("Execution createOrUpdateResource for: {}", resource.getMetadata().getName());
        dependentSecrets.onCreateOrUpdate(resource);
        DefaultStatus status = reconciledResourceProvider.setResourceWithStatus(resource);

        try {
            currentRealmService.runWithRealm(resource);

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

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Realm> context) {
        var configMapEventSource =
                new InformerEventSource<>(InformerConfiguration.from(Secret.class, context)
                        .withSecondaryToPrimaryMapper(dependentSecrets)
                        .withPrimaryToSecondaryMapper(dependentSecrets)
                        .build(),
                        context);
        return EventSourceInitializer.nameEventSources(configMapEventSource);
    }

    private Set<ResourceID> dependsOnSecrets(Realm realm) {
        var connection = realm.getSpec().getKeycloakConnection();
        return Stream.of(
                        connection.getClientSecretSecret(),
                        connection.getPasswordSecret())
                .filter(SecretRef::isValidRef)
                .map(ref -> ref.withDefaultNamespace(realm.getMetadata().getNamespace()))
                .map(ref -> new ResourceID(ref.getName(), ref.getNamespace()))
                .collect(Collectors.toSet());
    }
}
