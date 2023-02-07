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

package de.adorsys.keycloak.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xrodney.keycloak.operator.spec.DefaultStatus;
import com.github.xrodney.keycloak.operator.spec.KeycloakConnection;
import com.github.xrodney.keycloak.operator.spec.Realm;
import com.github.xrodney.keycloak.operator.spec.RealmSpec;
import de.adorsys.keycloak.config.configuration.ImportConfigPropertiesProvider;
import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.provider.KeycloakImportProvider;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.test.util.KeycloakAuthentication;
import de.adorsys.keycloak.config.test.util.KeycloakRepository;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.DefaultContext;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.Controller;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;

@ExtendWith(GithubActionsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Timeout(value = 30, unit = SECONDS)
abstract public class AbstractImportTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static Map<String, DefaultStatus> statuses = new HashMap<>();

    @Autowired
    public Operator operator;

    @Autowired
    public KeycloakImportProvider keycloakImportProvider;

    @Autowired
    public KeycloakProvider keycloakProvider;

    @Autowired
    public KeycloakRepository keycloakRepository;

    @Autowired
    public KeycloakAuthentication keycloakAuthentication;

    @Autowired
    public ImportConfigPropertiesProvider configPropertiesProvider;

    public String resourcePath;

    public DefaultStatus doImport(String fileName) throws IOException {
        Realm realm = getImport(fileName);
        return doImport(realm);
    }

    public <S extends DefaultStatus, P extends CustomResource<?, S>> S doImport(P realm) throws IOException {
        var controllers = operator.getRegisteredControllers();

        var maybeRegisteredController = controllers.stream()
                .filter(c -> c.getConfiguration().getResourceClass() == realm.getClass())
                .findFirst();

        var controller = (Controller<P>) maybeRegisteredController
                .orElseThrow(() -> new IllegalStateException("No registered controller for " + realm.getClass()));

        UpdateControl<P> result;
        try {
            result = controller.reconcile(realm,
                    new DefaultContext<>(null, controller, realm));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        if (result.isUpdateStatus()) {
            putStatus(realm);
        }

        return realm.getStatus();

//        if (realm.getStatus().getException() != null) {
//            throw realm.getStatus().getException();
//        }
    }

    @Deprecated
    public Realm getFirstImport(String fileName) throws IOException {
        return getImport(fileName);
    }

    public Realm getImport(String fileName) throws IOException {
        URL url = getClass().getClassLoader().getResource(this.resourcePath + '/' + fileName);
        if (url == null) {
            throw new IllegalArgumentException(fileName);
        }

        RealmRepresentation realmRepresentation = OBJECT_MAPPER.readValue(url, RealmImport.class);

        RealmSpec spec = new RealmSpec();
        spec.setRealm(realmRepresentation);
        spec.setImportProperties(configPropertiesProvider.getConfig());
        spec.setKeycloakConnection(KeycloakConnection.fromConfig(keycloakProvider.getProperties()));

        ObjectMeta meta = new ObjectMeta();
        meta.setName(realmRepresentation.getRealm());

        Realm realm = new Realm();
        realm.setSpec(spec);
        realm.setMetadata(meta);
        realm.setStatus(getStatus(realm));

        return realm;
    }

    public <T> T assertImportFails(Class<T> expectedException, String fileName) throws IOException {
        Realm realm = getImport(fileName);
        return assertImportFails(expectedException, realm);
    }

    public <T> T assertImportFails(Class<T> expectedException, Realm realm) throws IOException {
        DefaultStatus status = doImport(realm);
        Assertions.assertNotNull(status.getException());
        Assertions.assertTrue(expectedException.isInstance(status.getException()),
                () -> status.getException() + " should be instance of " + expectedException);
        return (T) status.getException();
    }

    private <P extends CustomResource<?, ? extends DefaultStatus>> void putStatus(P realm) {
        statuses.put(getCacheKey(realm), realm.getStatus());
    }

    private <P extends CustomResource<?, ? extends DefaultStatus>> DefaultStatus getStatus(P realm) {
        return statuses.get(getCacheKey(realm));
    }

    @NotNull
    private static <P extends CustomResource<?, ? extends DefaultStatus>> String getCacheKey(P realm) {
        return realm.getCRDName() + "/" + realm.getMetadata().getName();
    }
}
