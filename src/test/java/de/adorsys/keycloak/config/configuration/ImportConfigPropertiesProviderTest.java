package de.adorsys.keycloak.config.configuration;

import de.adorsys.keycloak.config.properties.ImmutableImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImmutableImportFilesProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ImportConfigPropertiesProviderTest {
    @Inject
    private ImportConfigProperties properties;

    @Inject
    private ImportConfigPropertiesProvider provider;

    @Test
    void propertiesProxyWorksCorrectly() {
        var defaultLocations = properties.getFiles().getLocations();
        assertNotNull(defaultLocations);
        assertEquals(defaultLocations, provider.getConfig().getFiles().getLocations());

        var editedLocations = List.of("location1a", "location1b");
        provider.editConfig(config -> ImmutableImportConfigProperties.builder().from(config)
                .files(ImmutableImportFilesProperties.builder().from(properties.getFiles())
                        .locations(editedLocations)
                        .build()
                )
                .build()
        );

        assertEquals(editedLocations, provider.getConfig().getFiles().getLocations());
        assertEquals(editedLocations, properties.getFiles().getLocations());

        provider.resetConfig();
        assertEquals(defaultLocations, provider.getConfig().getFiles().getLocations());
        assertEquals(defaultLocations, properties.getFiles().getLocations());
    }
}
