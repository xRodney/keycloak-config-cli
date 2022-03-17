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

package de.adorsys.keycloak.config.operator;

import de.adorsys.keycloak.config.operator.spec.SchemaSpec;
import de.adorsys.keycloak.config.operator.spec.SchemaStatus;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.NO_FINALIZER;

@ControllerConfiguration(finalizerName = NO_FINALIZER)
public class KeycloakConfigController implements Reconciler<KeycloakConfig> {
    public static final String KIND = "SchemaSpec";
    private static final Logger log = LoggerFactory.getLogger(KeycloakConfigController.class);

    private final KubernetesClient kubernetesClient;

    public KeycloakConfigController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public DeleteControl cleanup(KeycloakConfig resource, Context context) {
        log.info("Execution deleteResource for: {}", resource.getMetadata().getName());
        return DeleteControl.defaultDelete();
    }

    @Override
    public UpdateControl<KeycloakConfig> reconcile(KeycloakConfig resource, Context context) {
        try {
            log.info("Execution createOrUpdateResource for: {}", resource.getMetadata().getName());

            SchemaSpec.KeycloakConfigPropertiesSpec keycloakConnection = resource.getSpec().getKeycloakConnection();
            SchemaSpec.SecretRef secretRef = keycloakConnection.getCredentialSecret();

            Secret credentialSecret = kubernetesClient
                    .secrets()
                    .inNamespace(secretRef.getNamespace() == null ? resource.getMetadata().getNamespace() : secretRef.getNamespace())
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

            // runKeycloakConfig(resource, credentialSecret);

            SchemaStatus status = new SchemaStatus();
            status.setState(SchemaStatus.State.SUCCESS);
            status.setError(false);
            status.setMessage("Successful import");
            resource.setStatus(status);

            return UpdateControl.updateStatus(resource);
        } catch (Exception e) {
            log.error("Error while execute createOrUpdateResource", e);

            SchemaStatus status = new SchemaStatus();
            status.setState(SchemaStatus.State.ERROR);
            status.setError(true);
            status.setMessage(e.getMessage());
            resource.setStatus(status);

            return UpdateControl.updateStatus(resource);
        }
    }

    /*
    private void runKeycloakConfig(KeycloakConfig resource, Secret credentialSecret) throws IOException {
        String importConfigString = resource.getSpec().getRealmConfiguration();
        InputStream importConfigStream = new ByteArrayInputStream(importConfigString.getBytes(StandardCharsets.UTF_8));
        File importConfig = FileUtils.createTempFile("spec", importConfigStream);
        File springConfig = generateSpringProperties(resource, credentialSecret);

        KeycloakConfigApplication.run(new String[]{
                "--spring.config.location=classpath:,file:" + springConfig.getAbsolutePath(),
                "--import.path=" + importConfig.getAbsolutePath(),
        });
    }

    private File generateSpringProperties(KeycloakConfig resource, Secret credentialSecret) throws IOException {
        HashMap<String, Object> springConfig = new HashMap<>();
        springConfig.put("keycloak", getKeycloakProperties(resource, credentialSecret));

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        InputStream springConfigStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(springConfig));

        return FileUtils.createTempFile("spring-config", springConfigStream);
    }

    private Object getKeycloakProperties(KeycloakConfig resource, Secret credentialSecret) throws MalformedURLException {
        URL url = new URL(resource.getSpec().getKeycloakConnection().getUrl());
        String username = credentialSecret.getStringData().getOrDefault("username", null);
        String password = credentialSecret.getStringData().getOrDefault("password", null);
        String clientSecret = credentialSecret.getStringData().getOrDefault("client-secret", null);

        return new KeycloakConfigProperties(
                resource.getSpec().getKeycloakConnection().getLoginRealm(),
                resource.getSpec().getKeycloakConnection().getClientId(),
                url,
                username,
                password,
                clientSecret,
                resource.getSpec().getKeycloakConnection().getGrantType(),
                resource.getSpec().getKeycloakConnection().isSslVerify(),
                null,
                resource.getSpec().getKeycloakConnection().getAvailabilityCheck()
        );
    }

    private void runKeycloakConfig(KeycloakConfig resource, Secret credentialSecret) throws MalformedURLException, JsonProcessingException {
        KeycloakProvider keycloakProvider = connectToKeycloak(resource, credentialSecret);
        RealmImport realmImport = readRealmConfig(resource);
        realmImportService.doImport(realmImport.getValue());
    }

    private KeycloakProvider connectToKeycloak(KeycloakConfig resource, Secret credentialSecret) throws MalformedURLException {
        URL url = new URL(resource.getSpec().getKeycloakConnection().getUrl());
        String username = credentialSecret.getStringData().getOrDefault("username", null);
        String password = credentialSecret.getStringData().getOrDefault("password", null);
        String clientSecret = credentialSecret.getStringData().getOrDefault("client-secret", null);

        KeycloakConfigProperties keycloakConfigProperties = new KeycloakConfigProperties(
                resource.getSpec().getKeycloakConnection().getLoginRealm(),
                resource.getSpec().getKeycloakConnection().getClientId(),
                url,
                username,
                password,
                clientSecret,
                resource.getSpec().getKeycloakConnection().getGrantType(),
                resource.getSpec().getKeycloakConnection().isSslVerify(),
                null,
                resource.getSpec().getKeycloakConnection().getAvailabilityCheck()
        );

        return new KeycloakProvider(keycloakConfigProperties);
    }

    private RealmImport readRealmConfig(KeycloakConfig resource) throws JsonProcessingException {
        String importConfig = resource.getSpec().getRealmConfiguration();

        String checksum = ChecksumUtil.checksum(importConfig.getBytes(StandardCharsets.UTF_8));
        RealmImport realmImport = objectMapper.readValue(importConfig, RealmImport.class);
        realmImport.setChecksum(checksum);
        return realmImport;
    }
    */
}
