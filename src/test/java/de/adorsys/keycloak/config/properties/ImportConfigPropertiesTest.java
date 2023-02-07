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

import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(GithubActionsExtension.class)
@QuarkusTest
@TestProfile(ImportConfigPropertiesTest.TestConfiguration.class)
class ImportConfigPropertiesTest {

    @Autowired
    private ImportConfigProperties properties;

    @Test
    @SuppressWarnings({"java:S2699", "java:S5961"})
    void shouldPopulateConfigurationProperties() {
        assertThat(properties.isValidate(), is(false));
        //assertThat(properties.isParallel(), is(true));
//        assertThat(properties.getFiles().getLocations(), contains("other"));
//        assertThat(properties.getFiles().getExcludes(), contains("exclude1", "exclude2"));
//        assertThat(properties.getFiles().isIncludeHiddenFiles(), is(true));
        assertThat(properties.getVarSubstitution().isEnabled(), is(true));
        assertThat(properties.getVarSubstitution().isNested(), is(false));
        assertThat(properties.getVarSubstitution().isUndefinedIsError(), is(false));
        assertThat(properties.getVarSubstitution().getPrefix(), is("${"));
        assertThat(properties.getVarSubstitution().getSuffix(), is("}"));
//        assertThat(properties.getRemoteState().isEnabled(), is(false));
//        assertThat(properties.getRemoteState().getEncryptionKey(), is("password"));
//        assertThat(properties.getRemoteState().getEncryptionSalt(), is("0123456789ABCDEFabcdef"));
        assertThat(properties.getManaged().getAuthenticationFlow(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getGroup(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getRequiredAction(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClientScope(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getScopeMapping(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClientScopeMapping(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getComponent(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getSubComponent(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getIdentityProvider(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getIdentityProviderMapper(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getRole(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClient(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getManaged().getClientAuthorizationResources(), is(ImportManagedPropertiesValues.NO_DELETE));
        assertThat(properties.getBehaviors().isSyncUserFederation(), is(true));
        assertThat(properties.getBehaviors().isRemoveDefaultRoleFromUser(), is(true));
        assertThat(properties.getBehaviors().isSkipAttributesForFederatedUser(), is(true));
    }

    public static class TestConfiguration implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("import.validate", "false"),
                    Map.entry("import.var-substitution.enabled", "true"),
                    Map.entry("import.var-substitution.nested", "false"),
                    Map.entry("import.var-substitution.undefined-is-error", "false"),
                    Map.entry("import.var-substitution.prefix", "$${"),
                    Map.entry("import.var-substitution.suffix", "}"),
                    Map.entry("import.managed.authentication-flow", "no-delete"),
                    Map.entry("import.managed.group", "no-delete"),
                    Map.entry("import.managed.required-action", "no-delete"),
                    Map.entry("import.managed.client-scope", "no-delete"),
                    Map.entry("import.managed.scope-mapping", "no-delete"),
                    Map.entry("import.managed.client-scope-mapping", "no-delete"),
                    Map.entry("import.managed.component", "no-delete"),
                    Map.entry("import.managed.sub-component", "no-delete"),
                    Map.entry("import.managed.identity-provider", "no-delete"),
                    Map.entry("import.managed.identity-provider-mapper", "no-delete"),
                    Map.entry("import.managed.role", "no-delete"),
                    Map.entry("import.managed.client", "no-delete"),
                    Map.entry("import.managed.client-authorization-resources", "no-delete"),
                    Map.entry("import.behaviors.sync-user-federation", "true"),
                    Map.entry("import.behaviors.remove-default-role-from-user", "true"),
                    Map.entry("import.behaviors.skip-attributes-for-federated-user", "true")
            );
        }
    }
}
