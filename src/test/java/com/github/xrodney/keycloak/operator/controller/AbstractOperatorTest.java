package com.github.xrodney.keycloak.operator.controller;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.github.xrodney.keycloak.operator.spec.DefaultStatus;
import com.github.xrodney.keycloak.operator.spec.KeycloakConnection;
import de.adorsys.keycloak.config.extensions.KeycloakExtension;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.junit.QuarkusMock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import javax.inject.Inject;
import java.io.File;
import java.time.Duration;

public class AbstractOperatorTest {
    protected static final ApiServerContainer<?> kubernetes = new ApiServerContainer<>();
    protected static final String REALM = "testrealm";
    protected static final String NAMESPACE = "testnamespace";

    @RegisterExtension
    protected static KeycloakExtension keycloakExtension = new KeycloakExtension();
    protected static KubernetesClient kubernetesClient;
    @Inject
    protected KeycloakProvider keycloakProvider;

    @BeforeAll
    static void setUpClass() {
        kubernetes.start();
        var kubeconfig = Config.fromKubeconfig(kubernetes.getKubeconfig());
        kubernetesClient = new KubernetesClientBuilder().withConfig(kubeconfig).build();
        QuarkusMock.installMockForType(kubernetesClient, KubernetesClient.class);

        kubernetesClient.namespaces().resource(new NamespaceBuilder().withNewMetadata().withName(NAMESPACE).endMetadata().build()).create();

        kubernetesClient.resources(CustomResourceDefinition.class)
                .load(new File("target/kubernetes/realms.keycloak-config.xrodney.github.com-v1.yml")).create();

    }

    protected void awaitResourceSuccess(CustomResource<?, ? extends DefaultStatus> resource) {
        Awaitility.await().atMost(Duration.ofMinutes(10))
                .pollInterval(Duration.ofSeconds(1))
                .failFast(() -> getState(resource) == DefaultStatus.State.ERROR)
                .until(() -> getState(resource) == DefaultStatus.State.SUCCESS);
    }

    protected <T extends CustomResource<?, ? extends DefaultStatus>> DefaultStatus.State getState(T resource) {
        var metadata = resource.getMetadata();
        var instance = (T) kubernetesClient.resources(resource.getClass())
                .inNamespace(metadata.getNamespace())
                .withName(metadata.getName())
                .get();

        if (instance == null) {
            return null;
        }
        var status = instance.getStatus();
        if (status == null) {
            return null;
        }
        return status.getState();
    }

    @NotNull
    protected KeycloakConnection getKeycloakConnection() {
        return KeycloakConnection.fromConfig(keycloakProvider.getProperties());
    }
}
