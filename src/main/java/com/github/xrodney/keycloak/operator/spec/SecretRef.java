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

public class SecretRef {
    private String name;
    private String namespace;
    private String key;
    private String immediateValue;

    public static SecretRef ref(String name, String key) {
        SecretRef ref = new SecretRef();
        ref.setName(name);
        ref.setKey(key);
        return ref;
    }

    public static SecretRef ref(String namespace, String name, String key) {
        SecretRef ref = new SecretRef();
        ref.setNamespace(namespace);
        ref.setName(name);
        ref.setKey(key);
        return ref;
    }

    public static SecretRef immediate(String value) {
        SecretRef ref = new SecretRef();
        ref.setImmediateValue(value);
        return ref;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getImmediateValue() {
        return immediateValue;
    }

    public void setImmediateValue(String immediateValue) {
        this.immediateValue = immediateValue;
    }
}
