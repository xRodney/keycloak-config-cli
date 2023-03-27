package com.github.xrodney.keycloak.operator.service;

import com.github.xrodney.keycloak.operator.spec.*;
import de.adorsys.keycloak.config.configuration.ImportConfigPropertiesProvider;
import de.adorsys.keycloak.config.properties.ImmutableKeycloakConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.KeycloakConfigProperties;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import io.fabric8.kubernetes.client.CustomResource;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Singleton;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@Singleton
public class CurrentRealmService {
    private final KeycloakProvider keycloakProvider;
    private final ImportConfigPropertiesProvider importConfigPropertiesProvider;
    private final SecretsManager secretsManager;
    private final RealmsManager realmsManager;

    public CurrentRealmService(KeycloakProvider keycloakProvider, ImportConfigPropertiesProvider importConfigPropertiesProvider, SecretsManager secretsManager, RealmsManager realmsManager) {
        this.keycloakProvider = keycloakProvider;
        this.importConfigPropertiesProvider = importConfigPropertiesProvider;
        this.secretsManager = secretsManager;
        this.realmsManager = realmsManager;
    }

    @ActivateRequestContext
    public <R> R runWithRealm(Realm realm, Supplier<R> run) {
        importConfigPropertiesProvider.editConfig(config -> mergeConfig(realm.getSpec().getImportProperties(), config));
        keycloakProvider.editProperties(config -> mergeKeycloakConnection(realm, config));

        return run.get();
    }

    @ActivateRequestContext
    public <S extends DefaultStatus, R extends RealmDependentSpec, T extends CustomResource<R, S>> T
    runWithRealm(T resource, UnaryOperator<T> run) {

        var realm = realmsManager.loadRealmOrThrow(getRealmRef(resource));
        importConfigPropertiesProvider.editConfig(config -> mergeConfig(realm.getSpec().getImportProperties(), config));
        keycloakProvider.editProperties(config -> mergeKeycloakConnection(realm, config));

        return run.apply(resource);
    }

    private ImportConfigProperties mergeConfig(ImportConfigProperties realmConfig, ImportConfigProperties globalConfig) {
        return globalConfig;
    }

    private KeycloakConfigProperties mergeKeycloakConnection(Realm resource, KeycloakConfigProperties config) {
        KeycloakConnection realmConfig = resource.getSpec().getKeycloakConnection();
        String currentNamespace = resource.getMetadata().getNamespace();

        var builder = ImmutableKeycloakConfigProperties.builder().from(config);
        if (realmConfig.getLoginRealm() != null) {
            builder.loginRealm(realmConfig.getLoginRealm());
        }
        if (realmConfig.getClientId() != null) {
            builder.clientId(realmConfig.getClientId());
        }
        if (realmConfig.getUrl() != null) {
            builder.url(realmConfig.getUrl());
        }

        if (realmConfig.getUser() != null) {
            builder.user(realmConfig.getUser());
        }

        if (realmConfig.getPasswordSecret() != null) {
            builder.password(secretsManager.readPassword(realmConfig.getPasswordSecret().withDefaultNamespace(currentNamespace)));
        }

        if (realmConfig.getClientSecretSecret() != null) {
            builder.clientSecret(secretsManager.readPassword(realmConfig.getClientSecretSecret().withDefaultNamespace(currentNamespace)));
        }

        if (realmConfig.getGrantType() != null) {
            builder.grantType(realmConfig.getGrantType());
        }
        return builder.build();
    }

    private <T extends CustomResource<? extends RealmDependentSpec, ?>> RealmRef getRealmRef(T resource) {
        return resource.getSpec().getRealmRef().withDefaultNamespace(resource.getMetadata().getNamespace());
    }
}
