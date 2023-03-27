package com.github.xrodney.keycloak.operator.service;

import com.github.xrodney.keycloak.operator.spec.Realm;
import com.github.xrodney.keycloak.operator.spec.RealmRef;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@Singleton
public class RealmsManager {
    private static final Logger log = LoggerFactory.getLogger(RealmsManager.class);

    private final KubernetesClient kubernetesClient;

    public RealmsManager(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public Realm loadRealmOrThrow(RealmRef realmRef) {
        var realm = kubernetesClient.resources(Realm.class)
                .inNamespace(realmRef.getNamespace())
                .withName(realmRef.getName())
                .get();

        if (realm == null) {
            throw new IllegalStateException(String.format("Realm named %s does not exist in namespace %s.",
                    realmRef.getName(), realmRef.getNamespace()));
        }

        return realm;
    }
}
