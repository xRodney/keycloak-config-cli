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

package com.github.xrodney.keycloak.operator.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DefaultStatus extends ObservedGenerationAwareStatus {
    @PrinterColumn
    private String externalId;
    @PrinterColumn
    private State state = State.UNKNOWN;
    @PrinterColumn
    private String message;
    @PrinterColumn
    private long lastUpdate;
    @JsonIgnore
    private RuntimeException exception;
    private Map<String, List<String>> status;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void success(String externalId) {
        setState(DefaultStatus.State.SUCCESS);
        setMessage("Successful import");
        setExternalId(externalId);
        setLastUpdate();
    }

    public void failure(Exception e) {
        setState(DefaultStatus.State.ERROR);
        setLastUpdate();
        if (e instanceof RuntimeException) {
            setException((RuntimeException) e);
        }

        String message = e.getMessage();
        if (e instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) e;
            try {
                if (wae.getResponse().hasEntity()) {
                    String entity = wae.getResponse().readEntity(String.class);
                    message += ": " + entity;
                }
            } catch (Exception ignore) {
                // no op
            }
        }
        setMessage(message);
    }

    @NotNull
    public String externalIdOrDefault(@Nullable CustomResource<?, DefaultStatus> resource, @NotNull String defaultValue) {
        return resource == null || resource.getStatus() == null || resource.getStatus().getExternalId() == null
                ? defaultValue
                : resource.getStatus().getExternalId();
    }

    public Map<String, List<String>> getStatus() {
        return status;
    }

    public void setStatus(Map<String, List<String>> status) {
        this.status = status;
    }

    public RuntimeException getException() {
        return exception;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate() {
        this.lastUpdate = Instant.now().toEpochMilli();
    }

    public enum State {
        SUCCESS,
        ERROR,
        UNKNOWN
    }
}
