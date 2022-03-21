/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.keycloak.config.operator.scope;

import de.adorsys.keycloak.config.operator.KeycloakConfigController;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RealmImportScope implements Scope, AutoCloseable {
    public static final RealmImportScope SINGLETON = new RealmImportScope();
    private static final ThreadLocal<ImportAttributes> IMPORT_ATTRIBUTES = new NamedThreadLocal<ImportAttributes>("RealmImportScope") {
        @Override
        protected ImportAttributes initialValue() {
            return new ImportAttributes();
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigController.class);

    public static RealmImportScope runInScope(KeycloakConfigProperties keycloakConfigProperties) {
        closeThreadScope();
        IMPORT_ATTRIBUTES.get().keycloakProperties = keycloakConfigProperties;
        return SINGLETON;
    }

    private static void closeThreadScope() {
        ImportAttributes importAttributes = IMPORT_ATTRIBUTES.get();
        logger.info("close before end: contains keys: {}", importAttributes.scope.keySet());
        logger.info("close before end: callbacks contains keys: {}", importAttributes.destructionCallbacks.keySet());

        importAttributes.destructionCallbacks.forEach((key, callback) -> {
            logger.info("close before end: calling callback: {}", key);
            callback.run();
        });
        IMPORT_ATTRIBUTES.remove();
    }

    public static KeycloakConfigProperties getKeycloakProperties() {
        return Objects.requireNonNull(IMPORT_ATTRIBUTES.get().getKeycloakProperties(),
                "'IMPORT_ATTRIBUTES.get().getKeycloakProperties()' are not set");
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Map<String, Object> scope = IMPORT_ATTRIBUTES.get().getScope();
        // NOTE: Do NOT modify the following to use Map::computeIfAbsent. For details,
        // see https://github.com/spring-projects/spring-framework/issues/25801.
        Object scopedObject = scope.get(name);
        if (scopedObject == null) {
            scopedObject = objectFactory.getObject();
            scope.put(name, scopedObject);
        }
        return scopedObject;
    }

    @Override
    @Nullable
    public Object remove(String name) {
        Map<String, Object> scope = IMPORT_ATTRIBUTES.get().getScope();
        return scope.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        IMPORT_ATTRIBUTES.get().getDestructionCallbacks().put(name, callback);
    }

    @Override
    @Nullable
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return Thread.currentThread().getName();
    }

    @Override
    public void close() {
        closeThreadScope();
    }

    private static class ImportAttributes {
        private final Map<String, Object> scope = new HashMap<>();
        private final Map<String, Runnable> destructionCallbacks = new HashMap<>();
        private KeycloakConfigProperties keycloakProperties;

        public Map<String, Object> getScope() {
            return scope;
        }

        public Map<String, Runnable> getDestructionCallbacks() {
            return destructionCallbacks;
        }

        public KeycloakConfigProperties getKeycloakProperties() {
            return keycloakProperties;
        }
    }
}
