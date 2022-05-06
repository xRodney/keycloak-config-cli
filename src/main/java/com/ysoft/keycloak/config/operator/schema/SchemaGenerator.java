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

package com.ysoft.keycloak.config.operator.schema;

import com.ysoft.keycloak.config.operator.spec.KeycloakConfig;
import io.fabric8.crd.generator.CRDGenerator;
import io.fabric8.crd.generator.CustomResourceInfo;
import io.sundr.adapter.api.AdapterContext;
import io.sundr.adapter.api.Adapters;
import io.sundr.model.Property;
import io.sundr.model.TypeDef;
import io.sundr.model.repo.DefinitionRepository;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for CRD generation in runtime during reflection.
 * <p>
 * For the CR spec, the upstream {@link org.keycloak.representations.idm.RealmRepresentation} is used.
 * Unfortunately, this contains recursive data structures, which are not allowed in CRD schemas.
 * <p>
 * Traditionally, in Java operators the CRD schema generation happens at compile time, using
 * <a href="https://github.com/fabric8io/kubernetes-client/tree/master/crd-generator/apt">CRD Generator annotation processor</a>
 * Unfortunately, due to the recursive schema in the classes beyond our control, this cannot be done.
 * <p>
 * (Note: This problem is also dealt with in the official java keycloak-operator.
 * What they do is that they modify the data sources using a shell script.
 * They can do it because their operator lives in the same repo as the data.)
 * <p>
 * Here, we move the CRD generation to runtime, where we have more control of the structures used.
 * Currently, we just remove the problematic fields, but the approach allows for more advanced scenarios,
 * including dynamically replacing portions of the schema, unrolling first N levels etc.
 */
@Component
public class SchemaGenerator {
    public void generateSchema(Path outputPath)  {
        final CRDGenerator generator = new CRDGenerator();

        DefinitionRepository repository = DefinitionRepository.getRepository();
        sanitizeType(repository, GroupRepresentation.class, "subGroups");
        sanitizeType(repository, ResourceRepresentation.class, "owner", "scopes");
        sanitizeType(repository, ScopeRepresentation.class, "policies", "resources");
        sanitizeType(repository, ComponentExportRepresentation.class, "subComponents");

        CustomResourceInfo customResourceInfo = CustomResourceInfo.fromClass(KeycloakConfig.class);
        generator.customResources(customResourceInfo);
        generator.inOutputDir(outputPath.toAbsolutePath().toFile());
        generator.generate();
    }

    private void sanitizeType(DefinitionRepository repository, Class<?> clazz, String... ignoredProps) {
        TypeDef typeDef = Adapters.adaptType(clazz, AdapterContext.getContext());
        TypeDef sanitizedDef = getTypeWithIgnoredProperties(typeDef, ignoredProps);
        repository.register(sanitizedDef);
    }

    private TypeDef getTypeWithIgnoredProperties(TypeDef typeDef, String... ignored) {
        Objects.requireNonNull(typeDef, "'typeDef' cannot be null");
        return new TypeDef(typeDef.getKind(), typeDef.getPackageName(), typeDef.getName(),
                typeDef.getComments(), typeDef.getAnnotations(), typeDef.getExtendsList(),
                typeDef.getImplementsList(), typeDef.getParameters(), ignoreProperties(typeDef, ignored),
                typeDef.getConstructors(), typeDef.getMethods(), typeDef.getOuterTypeName(), typeDef.getInnerTypes(),
                typeDef.getModifiers(), typeDef.getAttributes()
        );
    }

    private List<Property> ignoreProperties(TypeDef typeDef, String... ignored) {
        return typeDef.getProperties().stream()
                .filter(prop -> Stream.of(ignored).noneMatch(i -> i.equals(prop.getName())))
                .collect(Collectors.toList());
    }
}
