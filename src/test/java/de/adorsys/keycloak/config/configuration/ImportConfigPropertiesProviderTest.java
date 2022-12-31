package de.adorsys.keycloak.config.configuration;

import de.adorsys.keycloak.config.properties.ImmutableImportBehaviorsProperties;
import de.adorsys.keycloak.config.properties.ImmutableImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ImportConfigPropertiesProviderTest {
    @Inject
    private ImportConfigProperties properties;

    @Inject
    private ImportConfigPropertiesProvider provider;

    @Test
    void propertiesProxyWorksCorrectly() {
        var defaultSyncUserFederation = properties.getBehaviors().isSyncUserFederation();
        assertEquals(defaultSyncUserFederation, provider.getConfig().getBehaviors().isSyncUserFederation());

        var editedSyncUserFederation = !defaultSyncUserFederation;
        provider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .behaviors(ImmutableImportBehaviorsProperties.builder().from(properties.getBehaviors())
                        .isSyncUserFederation(editedSyncUserFederation)
                        .build())
                .build()
        );

        assertEquals(editedSyncUserFederation, provider.getConfig().getBehaviors().isSyncUserFederation());
        assertEquals(editedSyncUserFederation, properties.getBehaviors().isSyncUserFederation());

        provider.resetConfig();
        assertEquals(defaultSyncUserFederation, provider.getConfig().getBehaviors().isSyncUserFederation());
        assertEquals(defaultSyncUserFederation, properties.getBehaviors().isSyncUserFederation());
    }
}
