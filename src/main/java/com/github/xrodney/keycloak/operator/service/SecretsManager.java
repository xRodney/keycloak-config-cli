package com.github.xrodney.keycloak.operator.service;

import com.github.xrodney.keycloak.operator.spec.SecretRef;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Base64;

@Singleton
public class SecretsManager {
    private static final Logger log = LoggerFactory.getLogger(SecretsManager.class);

    private final KubernetesClient kubernetesClient;

    public SecretsManager(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public String readPassword(SecretRef secretRef) {
        if (StringUtils.isNotEmpty(secretRef.getImmediateValue())) {
            return secretRef.getImmediateValue();
        }

        Secret credentialSecret = kubernetesClient
                .secrets()
                .inNamespace(secretRef.getNamespace())
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
