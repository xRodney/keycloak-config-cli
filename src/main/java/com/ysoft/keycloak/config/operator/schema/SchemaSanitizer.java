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

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.crd.generator.CRDGenerator;
import io.fabric8.crd.generator.CustomResourceInfo;
import io.fabric8.kubernetes.client.CustomResource;
import io.sundr.adapter.api.AdapterContext;
import io.sundr.adapter.api.Adapters;
import io.sundr.model.ClassRef;
import io.sundr.model.Property;
import io.sundr.model.TypeDef;
import io.sundr.model.TypeRef;
import io.sundr.model.repo.DefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
 * Recursive data classes are un rolled by a configurable depth (by default 10)
 */

public class SchemaSanitizer {
    private static final ClassRef JSON_NODE_REF = ClassRef.forName(JsonNode.class.getCanonicalName());
    private static final Logger log = LoggerFactory.getLogger(SchemaSanitizer.class);
    private final CRDGenerator generator;
    private int maxRecursionDepth = 10;
    private final Map<String, Set<String>> ignoredPropsByClassName = new HashMap<>();
    private final DefinitionRepository repository;

    private VisitedTracker tracker;

    public SchemaSanitizer() {
        repository = DefinitionRepository.getRepository();
        generator = new CRDGenerator();
    }

    public void setIgnoredProps(Class<?> clazz, Collection<String> ignored) {
        ignoredPropsByClassName.computeIfAbsent(clazz.getCanonicalName(), key -> new HashSet<>()).addAll(ignored);
    }

    public void addResource(Class<? extends CustomResource<?, ?>> configClass) {
        CustomResourceInfo customResourceInfo = CustomResourceInfo.fromClass(configClass);
        tracker = new VisitedTracker(maxRecursionDepth);
        registerSanitizedType(customResourceInfo.definition(), customResourceInfo.definition().getName());
        generator.customResources(customResourceInfo);
    }

    public void generateSchema(Path outputPath) {
        generator.inOutputDir(outputPath.toAbsolutePath().toFile());
        generator.generate();
    }

    private void registerSanitizedType(Class<?> clazz) {
        TypeDef typeDef = Adapters.adaptType(clazz, AdapterContext.getContext());
        tracker = new VisitedTracker(maxRecursionDepth);
        registerSanitizedType(typeDef, typeDef.getName());
    }

    private TypeDef registerSanitizedType(TypeDef typeDef, String name) {
        Objects.requireNonNull(typeDef, "'typeDef' cannot be null");
        var sanitizedDef = new TypeDef(typeDef.getKind(), typeDef.getPackageName(), name,
                typeDef.getComments(), typeDef.getAnnotations(), sanitizeTypeRefs(typeDef.getExtendsList()),
                sanitizeTypeRefs(typeDef.getImplementsList()), typeDef.getParameters(), handleProperties(typeDef),
                typeDef.getConstructors(), typeDef.getMethods(), typeDef.getOuterTypeName(), typeDef.getInnerTypes(),
                typeDef.getModifiers(), typeDef.getAttributes()
        );
        log.debug("registerSanitizedType {}", sanitizedDef);
        repository.register(sanitizedDef);
        return sanitizedDef;
    }

    private List<Property> handleProperties(TypeDef typeDef) {
        return typeDef.getProperties().stream()
                .map(prop -> handleProperty(typeDef, prop))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Property handleProperty(TypeDef typeDef, Property prop) {
        if (isIgnoredProperty(typeDef, prop)) {
            return null;
        } else if (!isJavaInternal(prop.getTypeRef())) {
            TypeRef typeRef = getSanitizedTypeRef(prop.getTypeRef());
            return new Property(prop.getAnnotations(), typeRef, prop.getName(),
                    prop.getComments(), prop.getModifiers(), prop.getAttributes());
        }
        return prop;
    }

    private boolean isJavaInternal(TypeRef typeRef) {
        if (typeRef instanceof ClassRef) {
            var classRef = (ClassRef) typeRef;
            var fullyQualifiedName = classRef.getFullyQualifiedName();
            if (isJavaInternal(fullyQualifiedName) && classRef.getArguments().stream().allMatch(this::isJavaInternal)) {
                return true;
            }
            if (isJsonNode(classRef)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJavaInternal(String fullyQualifiedName) {
        return fullyQualifiedName.startsWith("java.") || fullyQualifiedName.startsWith("com.sun.");
    }

    private boolean isJsonNode(ClassRef classRef) {
        return classRef.equals(JSON_NODE_REF) || classRef.getFullyQualifiedName().startsWith(JSON_NODE_REF.getFullyQualifiedName());
    }


    private boolean isIgnoredProperty(TypeDef typeDef, Property prop) {
        return ignoredPropsByClassName.getOrDefault(typeDef.getFullyQualifiedName(), Set.of())
                .contains(prop.getName());
    }

    private <T extends TypeRef> T getSanitizedTypeRef(T typeRef) {
        if (isJavaInternal(typeRef)) {
            return typeRef;
        }
        if (typeRef instanceof ClassRef && tracker.shouldSkip(typeRef)) {
            return (T) JSON_NODE_REF;
        }

        var typeDef = repository.getDefinition(typeRef);
        if (typeDef == null || typeDef.isEnum()) {
            return typeRef;
        }

        try {
            int depth = tracker.push(typeRef);
            String name = depth == 0 ? typeDef.getName() : typeDef.getName() + depth;
            typeDef = registerSanitizedType(typeDef, name);
        } finally {
            tracker.pop();
        }

        if (typeRef instanceof ClassRef) {
            var arguments = sanitizeTypeRefs(((ClassRef) typeRef).getArguments());
            return (T) typeDef.toReference(arguments);
        }
        return typeRef;
    }

    private <T extends TypeRef> List<T> sanitizeTypeRefs(List<T> typeRefs) {
        return typeRefs.stream()
                .map(this::getSanitizedTypeRef)
                .collect(Collectors.toList());
    }

    public void setMaxRecursionDepth(int maxRecursionDepth) {
        this.maxRecursionDepth = maxRecursionDepth;
    }

    static class VisitedTracker {
        private final int maxRecursionDepth;
        private final Deque<TypeRef> path;
        private final Map<TypeRef, Integer> visited;
        private final Set<TypeRef> repeated;

        VisitedTracker(int maxRecursionDepth) {
            this.maxRecursionDepth = maxRecursionDepth;
            this.path = new ArrayDeque<>();
            this.visited = new HashMap<>();
            this.repeated = new HashSet<>();
        }

        public int getDepth(TypeRef typeRef) {
            return visited.getOrDefault(typeRef, 0);
        }

        public int push(TypeRef typeRef) {
            int depth = getDepth(typeRef);
            log.info("push depth {}: {}", typeRef, depth + 1);
            path.push(typeRef);
            visited.put(typeRef, depth + 1);
            if (depth != 0) {
                repeated.add(typeRef);
            }
            return depth;
        }

        public void pop() {
            var typeRef = path.pop();
            var depth = getDepth(typeRef) - 1;
            log.info("pop depth {}: {}", typeRef, depth);

            if (depth <= 1) {
                repeated.remove(typeRef);
            }
            visited.put(typeRef, depth);
        }

        public boolean shouldSkip(TypeRef typeRef) {
            int depth = getDepth(typeRef);
            if (depth >= maxRecursionDepth) {
                log.info("Skipping generation of {} - recursion too deep ({})", renderPath(typeRef), depth);
                return true;
            } else if (repeated.size() > 1) {
                log.info("Skipping generation of {} - recursion involving multiple classes {}", renderPath(typeRef), repeated);
                return true;
            }
            return false;
        }

        public String renderPath(TypeRef typeRef) {
            StringBuilder str = new StringBuilder();
            path.descendingIterator().forEachRemaining(td -> str.append(td).append(" -> "));
            str.append(typeRef);
            return str.toString();
        }
    }
}
