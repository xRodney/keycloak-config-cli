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

import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.springboot.starter.ResourceClassResolver;
import io.sundr.model.ClassRef;
import io.sundr.model.repo.DefinitionRepository;
import org.keycloak.common.util.MultivaluedHashMap;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class SchemaGenerator {
    private final Collection<Reconciler<?>> reconcilers;
    private final ResourceClassResolver resourceClassResolver;

    public SchemaGenerator(Collection<Reconciler<?>> reconcilers, ResourceClassResolver resourceClassResolver) {
        this.reconcilers = reconcilers;
        this.resourceClassResolver = resourceClassResolver;
    }

    public void run(Path target) {
        var schemaGenerator = new SchemaSanitizer();
        schemaGenerator.schemaFrom(MultivaluedHashMap.class, this::customizeMultivaluedHashMap);
        for (var reconciler : reconcilers) {
            var customResourceClass = resourceClassResolver.resolveCustomResourceClass(reconciler);
            schemaGenerator.addResource(customResourceClass);
        }
        schemaGenerator.generateSchema(target);
    }

    public ClassRef customizeMultivaluedHashMap(ClassRef classRef, DefinitionRepository repository) {
        var key = classRef.getArguments().get(0);
        var value = classRef.getArguments().get(1);

        var map = repository.getDefinition(Map.class.getCanonicalName());
        var list = repository.getDefinition(List.class.getCanonicalName());

        return map.toReference(key, list.toReference(value));
    }
}
