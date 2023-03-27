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

public class RealmRef {
    private final String name;
    private final String namespace;

    @JsonCreator
    public RealmRef(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public RealmRef withDefaultNamespace(String defaultNamespace) {
        return namespace == null ? new RealmRef(name, defaultNamespace) : this;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
