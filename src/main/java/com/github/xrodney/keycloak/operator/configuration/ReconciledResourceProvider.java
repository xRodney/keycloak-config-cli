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

package com.github.xrodney.keycloak.operator.configuration;

import com.github.xrodney.keycloak.operator.spec.DefaultStatus;
import io.fabric8.kubernetes.client.CustomResource;

import java.util.Objects;
import java.util.function.Supplier;
import javax.enterprise.context.RequestScoped;
import javax.validation.constraints.NotNull;

@RequestScoped
public class ReconciledResourceProvider {
    private CustomResource<?, ? extends DefaultStatus> resource;

    public CustomResource<?, ? extends DefaultStatus> getResource() {
        return resource;
    }

    public void setResource(CustomResource<?, ? extends DefaultStatus> resource) {
        Objects.requireNonNull(resource, "'resource' cannot be null");
        Objects.requireNonNull(resource.getStatus(), "'resource.getStatus()' cannot be null");
        this.resource = resource;
    }

    public <S extends DefaultStatus> S setResourceWithStatus(CustomResource<?, S> resource, Supplier<S> defaultStatusSupplier) {
        Objects.requireNonNull(resource, "'resource' cannot be null");
        if (resource.getStatus() == null) {
            resource.setStatus(defaultStatusSupplier.get());
        }
        this.resource = resource;
        return resource.getStatus();
    }

    @NotNull
    public DefaultStatus getStatus() {
        return Objects.requireNonNull(resource.getStatus(), "'resource.getStatus()' cannot be null");
    }

    public String getExternalId(String defaultId) {
        return getStatus().getExternalId() != null ? resource.getStatus().getExternalId() : defaultId;
    }
}
