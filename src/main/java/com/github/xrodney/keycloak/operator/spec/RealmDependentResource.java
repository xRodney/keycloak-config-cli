package com.github.xrodney.keycloak.operator.spec;

public abstract class RealmDependentResource<S extends RealmDependentSpec, T extends DefaultStatus>
        extends KeycloakResource<S, T> {
}
