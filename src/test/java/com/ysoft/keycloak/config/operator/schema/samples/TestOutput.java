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

package com.ysoft.keycloak.config.operator.schema.samples;

import io.fabric8.crd.generator.CRDGenerator;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;

public class TestOutput extends CRDGenerator.AbstractCRDOutput<ByteArrayOutputStream> {

    @Override
    protected ByteArrayOutputStream createStreamFor(String crdName) {
        return new ByteArrayOutputStream();
    }

    public CustomResourceDefinition getCustomResourceDefinition(String name) {
        Yaml yaml = new Yaml();
        ObjectMapper mapper = new ObjectMapper();
        var schema = yaml.load(getStreamFor(name).toString());
        return mapper.convertValue(schema, CustomResourceDefinition.class);
    }
}
