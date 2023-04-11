package com.github.xrodney.keycloak.operator.controller;

import com.github.xrodney.keycloak.operator.spec.Realm;
import com.github.xrodney.keycloak.operator.spec.SecretRef;
import com.github.xrodney.keycloak.operator.utils.TestDataGenerator;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import static org.junit.jupiter.api.Assertions.*;

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
        super.setUp();
        operator = new Operator(kubernetesClient);
        operator.register(realmController);
    }

    @Test
    void shouldCreateRealmUsingEmbeddedConnectionPassword() {
        Realm realm = TestDataGenerator.createDefaultRealm("realm1", getKeycloakConnection());
        var resource = kubernetesClient.resource(realm).inNamespace(NAMESPACE).create();

        operator.start();
        awaitResourceSuccess(resource);
        operator.stop();

        var export = keycloakProvider.getInstance().realm("realm1").partialExport(false, false);
        assertNotNull(export);
    }

    @Test
    void shouldCreateRealmUsingReferencedConnectionPassword() {
        var connection = getKeycloakConnection();
        Realm realm = TestDataGenerator.createDefaultRealm("realm2", connection);

        var secretRef = SecretRef.ref(NAMESPACE, "test2", "mykey");
        var secret = TestDataGenerator.secretFromReference(secretRef, connection.getPasswordSecret().getImmediateValue());
        connection.setPasswordSecret(secretRef);

        kubernetesClient.resource(secret).create();
        var resource = kubernetesClient.resource(realm).inNamespace(NAMESPACE).create();

        operator.start();
        awaitResourceSuccess(resource);
        operator.stop();

        var export = keycloakProvider.getInstance().realm("realm2").partialExport(false, false);
        assertNotNull(export);
    }

    @Test
    void shouldFailWhenRealmReferencesNonExistentSecret() {
        var connection = getKeycloakConnection();
        Realm realm = TestDataGenerator.createDefaultRealm("realm3", connection);

        var secretRef = SecretRef.ref(NAMESPACE, "test3", "mykey");
        connection.setPasswordSecret(secretRef);

        var resource = kubernetesClient.resource(realm).inNamespace(NAMESPACE).create();

        operator.start();
        var awaited = awaitResourceError(resource);
        operator.stop();

        assertEquals("The linked credential secret 'test3' in namespace 'testnamespace' does not exist.",
                awaited.getStatus().getMessage());


        assertThrows(NotFoundException.class, () -> keycloakProvider.getInstance().realm("realm3")
                .partialExport(false, false));
    }

    @Test
    void shouldFailWhenRealmReferencedSecretDoesNotContainKey() {
        var connection = getKeycloakConnection();
        Realm realm = TestDataGenerator.createDefaultRealm("realm4", connection);

        var secretRef = SecretRef.ref(NAMESPACE, "test4", "mykey");
        var secret = TestDataGenerator.secretFromReference(secretRef, connection.getPasswordSecret().getImmediateValue());
        connection.setPasswordSecret(SecretRef.ref(NAMESPACE, "test4", "myotherkey"));

        kubernetesClient.resource(secret).create();
        var resource = kubernetesClient.resource(realm).inNamespace(NAMESPACE).create();

        operator.start();
        var awaited = awaitResourceError(resource);
        operator.stop();

        assertEquals("The linked credential secret 'test4' in namespace 'testnamespace' does not contain key 'myotherkey'.", awaited.getStatus().getMessage());

        assertThrows(NotFoundException.class, () -> keycloakProvider.getInstance().realm("realm4")
                .partialExport(false, false));
    }

    @Test
    void shouldRetryWhenRealmReferencesSecretThatIsCreatedAtLaterTime() {
        var connection = getKeycloakConnection();
        Realm realm = TestDataGenerator.createDefaultRealm("realm5", connection);

        var secretRef = SecretRef.ref(NAMESPACE, "test5", "mykey");
        var secret = TestDataGenerator.secretFromReference(secretRef, connection.getPasswordSecret().getImmediateValue());
        connection.setPasswordSecret(secretRef);

        var resource = kubernetesClient.resource(realm).inNamespace(NAMESPACE).create();

        operator.start();
        var awaited = awaitResourceError(resource);
        assertEquals("The linked credential secret 'test5' in namespace 'testnamespace' does not exist.",
                awaited.getStatus().getMessage());

        kubernetesClient.resource(secret).create();

        awaitResourceSuccess(resource, awaited.getStatus().getLastUpdate());
        operator.stop();
    }
}
