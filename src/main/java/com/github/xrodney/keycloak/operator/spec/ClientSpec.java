package com.github.xrodney.keycloak.operator.spec;

import io.fabric8.crd.generator.annotation.SchemaSwap;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

@SchemaSwap(originalType = ScopeRepresentation.class, fieldName = "policies")
@SchemaSwap(originalType = ScopeRepresentation.class, fieldName = "resources")
public class ClientSpec implements RealmDependentSpec {
    private RealmRef realmRef;
    private ClientRepresentation client;
    private SecretRef clientSecretRef;

    @Override
    public RealmRef getRealmRef() {
        return realmRef;
    }

    public void setRealmRef(RealmRef realmRef) {
        this.realmRef = realmRef;
    }

    public ClientRepresentation getClient() {
        return client;
    }

    public void setClient(ClientRepresentation client) {
        this.client = client;
    }

    public SecretRef getClientSecretRef() {
        return clientSecretRef;
    }

    public void setClientSecretRef(SecretRef clientSecretRef) {
        this.clientSecretRef = clientSecretRef;
    }
}
