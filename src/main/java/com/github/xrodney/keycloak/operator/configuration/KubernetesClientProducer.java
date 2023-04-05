package com.github.xrodney.keycloak.operator.configuration;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Singleton
public class KubernetesClientProducer {

    /**
     * Produce the default autoconfigured k8s client
     * Some tests may choose to override it.
     */
    @Produces
    @ApplicationScoped
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
