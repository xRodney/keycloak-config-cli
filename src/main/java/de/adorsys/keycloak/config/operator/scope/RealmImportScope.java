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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RealmImportScope implements Scope, AutoCloseable {
    public static final ThreadLocal<Map<String, Object>> THREAD_SCOPE = new NamedThreadLocal<Map<String, Object>>("SimpleThreadScope") {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };
    public static final ThreadLocal<Map<String, Runnable>> DESTRUCTION_CALLBACKS = new NamedThreadLocal<Map<String, Runnable>>(
            "SimpleThreadScope-callbacks") {
        @Override
        protected Map<String, Runnable> initialValue() {
            return new HashMap<>();
        }
    };
    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfigController.class);

    public static RealmImportScope runInScope() {
        closeThreadScope();

        logger.info("runInScope before end: contains keys: {}", THREAD_SCOPE.get().keySet());
        logger.info("runInScope before end: callback contains keys: {}", DESTRUCTION_CALLBACKS.get().keySet());
        return new RealmImportScope();
    }

    private static void closeThreadScope() {
        logger.info("close before end: contains keys: {}", THREAD_SCOPE.get().keySet());
        logger.info("close before end: callbacks contains keys: {}", DESTRUCTION_CALLBACKS.get().keySet());

        for (Map.Entry<String, Runnable> entry : DESTRUCTION_CALLBACKS.get().entrySet()) {
            logger.info("close before end: calling callback: {}", entry.getKey());
            entry.getValue().run();
        }
        DESTRUCTION_CALLBACKS.get().clear();
        THREAD_SCOPE.get().clear();

        logger.info("close after end: contains keys: {}", THREAD_SCOPE.get().keySet());
        logger.info("close after end: callbacks contains keys: {}", DESTRUCTION_CALLBACKS.get().keySet());
    }

    public void put(String name, Object value) {
        THREAD_SCOPE.get().put(name, value);
        if (value instanceof AutoCloseable) {
            DESTRUCTION_CALLBACKS.get().put(name, () -> safeClose((AutoCloseable) value));
        }
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Map<String, Object> scope = this.THREAD_SCOPE.get();
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
        Map<String, Object> scope = this.THREAD_SCOPE.get();
        return scope.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        DESTRUCTION_CALLBACKS.get().put(name, callback);
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

    private static void safeClose(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
