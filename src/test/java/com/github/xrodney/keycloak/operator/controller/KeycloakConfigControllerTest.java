package com.github.xrodney.keycloak.operator.controller;

import com.github.xrodney.keycloak.operator.spec.Realm;
import com.github.xrodney.keycloak.operator.spec.RealmSpec;
import com.github.xrodney.keycloak.operator.spec.SecretRef;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import javax.inject.Inject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/*
 *
 * simple success
 * keycloak not available -> retry
 * keycloak connection secret does not exist -> retry (maybe hook at the secret creation?)

 * */

@QuarkusTest
class KeycloakConfigControllerTest extends AbstractOperatorTest {

    protected Operator operator;

    @Inject
    protected KeycloakConfigController realmController;

    @BeforeEach
    void setUp() {
        operator = new Operator(kubernetesClient);
        operator.register(realmController);
    }

    @Test
    void shouldCreateRealmUsingEmbeddedConnectionPassword() {
        var realm = new Realm();
        realm.setMetadata(new ObjectMetaBuilder().withName(REALM).build());

        RealmSpec spec = new RealmSpec();
        spec.setKeycloakConnection(getKeycloakConnection());
        var representation = new RealmRepresentation();
        representation.setRealm(REALM);
        spec.setRealm(representation);
        realm.setSpec(spec);

        var resource = kubernetesClient.resource(realm).inNamespace(NAMESPACE);
        var created = resource.create();

        operator.start();
        awaitResourceSuccess(created);
        operator.stop();

        var export = keycloakProvider.getInstance().realm(REALM).partialExport(false, false);
        assertNotNull(export);
    }

    @Test
    void shouldCreateRealmUsingReferencedConnectionPassword() {
        var realm = new Realm();
        realm.setMetadata(new ObjectMetaBuilder().withName(REALM).build());

        RealmSpec spec = new RealmSpec();
        var connection = getKeycloakConnection();

        var secret = new SecretBuilder().withNewMetadata()
                .withNamespace(NAMESPACE)
                .withName("mysecret")
                .endMetadata()
                .withStringData(Map.of("mykey", connection.getPasswordSecret().getImmediateValue()))
                .build();
        kubernetesClient.resource(secret).create();
        var secretRef = SecretRef.ref("mysecret", "mykey");
        connection.setPasswordSecret(secretRef);
        spec.setKeycloakConnection(connection);

        var representation = new RealmRepresentation();
        representation.setRealm(REALM);
        spec.setRealm(representation);
        realm.setSpec(spec);

        var resource = kubernetesClient.resource(realm).inNamespace(NAMESPACE);
        var created = resource.create();

        operator.start();
        awaitResourceSuccess(created);
        operator.stop();

        var export = keycloakProvider.getInstance().realm(REALM).partialExport(false, false);
        assertNotNull(export);
    }
}
