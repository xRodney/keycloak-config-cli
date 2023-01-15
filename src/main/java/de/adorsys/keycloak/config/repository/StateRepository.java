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

package de.adorsys.keycloak.config.repository;

import com.github.xrodney.keycloak.operator.configuration.ReconciledResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class StateRepository {
    private static Logger logger = LoggerFactory.getLogger(StateRepository.class);

    private final ReconciledResourceProvider resourceProvider;
    private Map<String, List<String>> attributes;

    public StateRepository(ReconciledResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public void loadCustomAttributes() {
        var resourceAttributes = resourceProvider.getStatus().getStatus();
        attributes = resourceAttributes == null ? new HashMap<>() : new HashMap<>(resourceAttributes);
        logger.debug("loadCustomAttributes {}", attributes);
    }

    public List<String> getState(String entity) {
        var list = attributes.getOrDefault(entity, Collections.emptyList());
        logger.debug("getState {} -> {}", entity, list);
        return list;
    }

    public void update() {
        logger.debug("update {}", attributes);
        resourceProvider.getStatus().setStatus(attributes);
    }

    public void setState(String entity, List<String> values) {
        logger.debug("getState {} -> {}", entity, values);
        attributes.put(entity, values);
    }
}
