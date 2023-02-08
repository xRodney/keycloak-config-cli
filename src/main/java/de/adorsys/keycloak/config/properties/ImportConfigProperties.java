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

package de.adorsys.keycloak.config.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import org.immutables.value.Value;

@ConfigMapping(prefix = "import")
@SuppressWarnings({"java:S107"})
@Value.Immutable
public interface ImportConfigProperties {

    @WithName("validate")
    boolean isValidate();

    @WithName("behaviors")
    ImportBehaviorsProperties getBehaviors();

    @WithName("managed")
    ImportManagedProperties getManaged();

    @SuppressWarnings("unused")
    @Value.Immutable
    interface ImportManagedProperties {
        @WithName("required-action")
        ImportManagedPropertiesValues getRequiredAction();

        @WithName("group")
        ImportManagedPropertiesValues getGroup();

        @WithName("client-scope")
        ImportManagedPropertiesValues getClientScope();

        @WithName("scope-mapping")
        ImportManagedPropertiesValues getScopeMapping();

        @WithName("client-scope-mapping")
        ImportManagedPropertiesValues getClientScopeMapping();

        @WithName("component")
        ImportManagedPropertiesValues getComponent();

        @WithName("sub-component")
        ImportManagedPropertiesValues getSubComponent();

        @WithName("authentication-flow")
        ImportManagedPropertiesValues getAuthenticationFlow();

        @WithName("identity-provider")
        ImportManagedPropertiesValues getIdentityProvider();

        @WithName("identity-provider-mapper")
        ImportManagedPropertiesValues getIdentityProviderMapper();

        @WithName("role")
        ImportManagedPropertiesValues getRole();

        @WithName("client")
        ImportManagedPropertiesValues getClient();

        @WithName("client-authorization-resources")
        ImportManagedPropertiesValues getClientAuthorizationResources();

        public enum ImportManagedPropertiesValues {
            FULL, NO_DELETE
        }
    }

    @SuppressWarnings("unused")
    @Value.Immutable
    interface ImportBehaviorsProperties {
        @WithName("sync-user-federation")
        boolean isSyncUserFederation();

        @WithName("remove-default-role-from-user")
        boolean isRemoveDefaultRoleFromUser();

        @WithName("skip-attributes-for-federated-user")
        boolean isSkipAttributesForFederatedUser();
    }
}
