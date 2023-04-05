package com.github.xrodney.keycloak.operator.utils;

import com.github.xrodney.keycloak.operator.spec.KeycloakConnection;
import com.github.xrodney.keycloak.operator.spec.Realm;
import com.github.xrodney.keycloak.operator.spec.RealmSpec;
import com.github.xrodney.keycloak.operator.spec.SecretRef;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.jetbrains.annotations.NotNull;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Map;

public class TestDataGenerator {
    public static Realm createDefaultRealm(String name, @NotNull KeycloakConnection keycloakConnection) {
        var realm = new Realm();
        realm.setMetadata(new ObjectMetaBuilder().withName(name).build());

        var representation = new RealmRepresentation();
        representation.setRealm(name);

        RealmSpec spec = new RealmSpec();
        spec.setKeycloakConnection(keycloakConnection);
        spec.setRealm(representation);
        realm.setSpec(spec);

        return realm;
    }

    public static Secret secretFromReference(SecretRef ref, String value) {
        return new SecretBuilder().withNewMetadata()
                .withNamespace(ref.getNamespace())
                .withName(ref.getName())
                .endMetadata()
                .withStringData(Map.of(ref.getKey(), value))
                .build();
    }
}
