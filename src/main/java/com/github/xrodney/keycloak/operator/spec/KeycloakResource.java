package com.github.xrodney.keycloak.operator.spec;

import io.fabric8.kubernetes.client.CustomResource;

public abstract class KeycloakResource<S, T extends DefaultStatus> extends CustomResource<S, T> {
    protected abstract T newStatus();

    /**
     * If the status is null, set it to value returned from {@link #newStatus()}.
     *
     * @apiNote This is different from {@link #initStatus()}, because that is called by the constructor,
     * meaning that the status is never null. With this method, the status is null on a fresh instance and has to be
     * initialized explicitly. This way, the patchStatus works correctly.
     */
    public void initDefaultStatus() {
        if (getStatus() == null) {
            setStatus(newStatus());
        }
    }
}
