package com.github.xrodney.keycloak.operator.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DependentResourcesMapper<PRIMARY extends HasMetadata, SECONDARY extends HasMetadata>
        implements SecondaryToPrimaryMapper<SECONDARY>, PrimaryToSecondaryMapper<PRIMARY> {

    private final Map<ResourceID, Set<ResourceID>> index = new HashMap<>();
    private final PrimaryToSecondaryMapper<PRIMARY> mapper;

    public DependentResourcesMapper(PrimaryToSecondaryMapper<PRIMARY> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Set<ResourceID> toSecondaryResourceIDs(PRIMARY primary) {
        return mapper.toSecondaryResourceIDs(primary);
    }

    @Override
    public Set<ResourceID> toPrimaryResourceIDs(SECONDARY dependentResource) {
        return index.getOrDefault(ResourceID.fromResource(dependentResource), Set.of());
    }

    public void onCreateOrUpdate(PRIMARY primary) {
        var id = ResourceID.fromResource(primary);
        toSecondaryResourceIDs(primary).forEach(s -> index.computeIfAbsent(s, k -> new HashSet<>()).add(id));
    }

    public void onDelete(PRIMARY primary) {
        var id = ResourceID.fromResource(primary);
        toSecondaryResourceIDs(primary).forEach(s -> {
            if (index.containsKey(s)) {
                index.get(s).remove(id);
            }
        });
    }
}
