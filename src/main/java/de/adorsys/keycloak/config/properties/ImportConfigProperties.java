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
    String REALM_STATE_ATTRIBUTE_COMMON_PREFIX = "de.adorsys.keycloak.config";
    String REALM_CHECKSUM_ATTRIBUTE_PREFIX_KEY = REALM_STATE_ATTRIBUTE_COMMON_PREFIX + ".import-checksum-{0}";
    String REALM_STATE_ATTRIBUTE_PREFIX_KEY = REALM_STATE_ATTRIBUTE_COMMON_PREFIX + ".state-{0}-{1}";

    @WithName("validate")
    boolean isValidate();

    @WithName("var-substitution")
    ImportVarSubstitutionProperties getVarSubstitution();

    @WithName("behaviors")
    ImportBehaviorsProperties getBehaviors();

    @WithName("cache")
    ImportCacheProperties getCache();

    @WithName("managed")
    ImportManagedProperties getManaged();

    @WithName("remote-state")
    ImportRemoteStateProperties getRemoteState();

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
    interface ImportVarSubstitutionProperties {
        @WithName("enabled")
        boolean isEnabled();

        @WithName("nested")
        boolean isNested();

        @WithName("undefined-is-error")
        boolean isUndefinedIsError();

        @WithName("prefix")
        String getPrefix();

        @WithName("suffix")
        String getSuffix();
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

    @SuppressWarnings("unused")
    @Value.Immutable
    interface ImportCacheProperties {
        @WithName("enabled")
        boolean isEnabled();

        @WithName("key")
        String getKey();
    }

    @SuppressWarnings("unused")
    @Value.Immutable
    interface ImportRemoteStateProperties {
        @WithName("enabled")
        boolean isEnabled();

        @WithName("encryption-key")
        String getEncryptionKey();

        @WithName("encryption-salt")
        String getEncryptionSalt();
    }
}
