package com.ysoft.keycloak.config.operator.schema;

import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.springboot.starter.ResourceClassResolver;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collection;

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
        for (var reconciler : reconcilers) {
            var customResourceClass = resourceClassResolver.resolveCustomResourceClass(reconciler);
            schemaGenerator.addResource(customResourceClass);
        }
        schemaGenerator.generateSchema(target);
    }
}
