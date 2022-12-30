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

package de.adorsys.keycloak.config.provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.keycloak.config.configuration.PathScanner;
import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.ImportResource;
import de.adorsys.keycloak.config.model.KeycloakImport;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.enterprise.context.control.ActivateRequestContext;

@Component
public class KeycloakImportProvider {
    private final Config config;
    private final PathScanner pathScanner;
    private final ImportConfigProperties importConfigProperties;

    private StringSubstitutor interpolator = null;

    private static final Logger logger = LoggerFactory.getLogger(KeycloakImportProvider.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Autowired
    public KeycloakImportProvider(
            Config config,
            PathScanner pathScanner,
            ImportConfigProperties importConfigProperties
    ) {
        this.config = config;
        this.pathScanner = pathScanner;
        this.importConfigProperties = importConfigProperties;
    }

    private void setupVariableSubstitution(Config config) {
        StringLookup variableResolver = StringLookupFactory.INSTANCE.interpolatorStringLookup(
                StringLookupFactory.INSTANCE.functionStringLookup(
                        propertyName -> config.getOptionalValue(propertyName, String.class).orElse(null))
        );

        this.interpolator = StringSubstitutor.createInterpolator()
                .setVariableResolver(variableResolver)
                .setVariablePrefix(importConfigProperties.getVarSubstitution().getPrefix())
                .setVariableSuffix(importConfigProperties.getVarSubstitution().getSuffix())
                .setEnableSubstitutionInVariables(importConfigProperties.getVarSubstitution().isNested())
                .setEnableUndefinedVariableException(importConfigProperties.getVarSubstitution().isUndefinedIsError());
    }

    public KeycloakImport readFromLocations(String... locations) {
        return readFromLocations(Arrays.asList(locations));
    }

    @ActivateRequestContext
    public KeycloakImport readFromLocations(Collection<String> locations) {
        if (importConfigProperties.getVarSubstitution().isEnabled()) {
            setupVariableSubstitution(config);
        }

        Map<String, Map<String, List<RealmImport>>> realmImports = new LinkedHashMap<>();

        for (String location : locations) {
            logger.debug("Loading file location '{}'", location);

            List<Resource> resources;
            try {
                resources = this.pathScanner.getResources(location);
            } catch (IOException | URISyntaxException e) {
                throw new InvalidImportException("Error loading resources", e);
            }

            if (resources.isEmpty()) {
                throw new InvalidImportException("No files matching '" + location + "'!");
            }

            // Import Pipe
            Map<String, List<RealmImport>> realmImport = resources.stream()
                    .map(this::readResource)
                    .filter(this::filterEmptyResources)
                    .sorted(Map.Entry.comparingByKey())
                    .map(this::substituteImportResource)
                    .map(this::readRealmImportFromImportResource)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            realmImports.put(location, realmImport);
        }

        return new KeycloakImport(realmImports);
    }

    private ImportResource readResource(Resource resource) {
        logger.debug("Loading file '{}'", resource.getFilename());

        try {
            try (InputStream inputStream = resource.getInputStream()) {
                return new ImportResource(resource.getURI().toString(), new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new InvalidImportException("Unable to proceed resource '" + resource + "': " + e.getMessage(), e);
        } finally {
            Authenticator.setDefault(null);
        }
    }

    private boolean filterEmptyResources(ImportResource resource) {
        return !resource.getValue().isEmpty();
    }

    private ImportResource substituteImportResource(ImportResource importResource) {
        if (importConfigProperties.getVarSubstitution().isEnabled()) {
            importResource.setValue(interpolator.replace(importResource.getValue()));
        }

        return importResource;
    }

    private Pair<String, List<RealmImport>> readRealmImportFromImportResource(ImportResource resource) {
        String location = resource.getFilename();
        String content = resource.getValue();
        String contentChecksum = DigestUtils.sha256Hex(content.replace("\r\n", "\n"));

        if (logger.isTraceEnabled()) {
            logger.trace(content);
        }

        List<RealmImport> realmImports;
        try {
            realmImports = readContent(content);
        } catch (Exception e) {
            throw new InvalidImportException("Unable to parse file '" + location + "': " + e.getMessage(), e);
        }
        realmImports.forEach(realmImport -> realmImport.setChecksum(contentChecksum));

        return new ImmutablePair<>(location, realmImports);
    }

    private List<RealmImport> readContent(String content) {
        List<RealmImport> realmImports = new ArrayList<>();

        Yaml yaml = new Yaml();
        Iterable<Object> yamlDocuments = yaml.loadAll(content);

        for (Object yamlDocument : yamlDocuments) {
            realmImports.add(OBJECT_MAPPER.convertValue(yamlDocument, RealmImport.class));
        }

        return realmImports;
    }
}
