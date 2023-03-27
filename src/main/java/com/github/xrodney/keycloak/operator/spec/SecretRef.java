/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2023 adorsys GmbH & Co. KG @ https://adorsys.com
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

package com.github.xrodney.keycloak.operator.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SecretRef {
    private final String name;
    private final String namespace;
    private final String key;
    private final String immediateValue;

    @JsonCreator
    SecretRef(@JsonProperty("name") String name,
              @JsonProperty("namespace") String namespace,
              @JsonProperty("key") String key,
              @JsonProperty("immediateValue") String immediateValue) {
        this.name = name;
        this.namespace = namespace;
        this.key = key;
        this.immediateValue = immediateValue;
    }

    public static SecretRef ref(String name, String key) {
        return new SecretRef(name, null, key, null);
    }

    public static SecretRef ref(String namespace, String name, String key) {
        return new SecretRef(name, namespace, key, null);
    }

    public static SecretRef immediate(String value) {
        return new SecretRef(null, null, null, value);
    }

    public SecretRef withDefaultNamespace(String defaultNamespace) {
        if (immediateValue != null || namespace != null) {
            return this;
        } else {
            return ref(defaultNamespace, name, key);
        }
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    public String getImmediateValue() {
        return immediateValue;
    }
}
