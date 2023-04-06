package com.github.xrodney.keycloak.operator.spec;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group(SchemaConstants.GROUP)
@Version(SchemaConstants.VERSION1)
public class Client extends RealmDependentResource<ClientSpec, DefaultStatus> implements Namespaced {
    @Override
    public DefaultStatus newStatus() {
        return new DefaultStatus();
    }
}
