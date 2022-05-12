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

import com.ysoft.keycloak.config.operator.schema.samples.ListRecursiveResource;
import com.ysoft.keycloak.config.operator.schema.samples.SimpleRecursiveResource;
import com.ysoft.keycloak.config.operator.schema.samples.TestOutput;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SchemaSanitizerTest {
    public static final int RECURSION_DEPTH = 2;
    private SchemaSanitizer sanitizer;
    private TestOutput output;

    @BeforeEach
    void setUp() {
        sanitizer = new SchemaSanitizer();
        sanitizer.setMaxRecursionDepth(RECURSION_DEPTH);
        output = new TestOutput();
    }

    @Test
    public void givenDirectRecursiveObject_firstNLevelsAreUnrolled() {
        sanitizer.addResource(SimpleRecursiveResource.class);
        sanitizer.generateSchema(output);

        JSONSchemaProps spec = getJsonSchema("simplerecursiveresources.test-v1");

        assertRecursion(spec, RECURSION_DEPTH, "child");
    }

    @Test
    public void givenListRecursiveObject_firstNLevelsAreUnrolled() {
        sanitizer.addResource(ListRecursiveResource.class);
        sanitizer.generateSchema(output);

        JSONSchemaProps spec = getJsonSchema("listrecursiveresources.test-v1");

        var additionalProperties = spec.getProperties().get("children").getItems().getAdditionalProperties();
        assertListRecursion(additionalProperties, RECURSION_DEPTH, "children");
    }

    private JSONSchemaProps getJsonSchema(String name) {
        var crd = output.getCustomResourceDefinition(name);
        Assertions.assertNotNull(crd);
        return crd.getSpec().getVersions().get(0).getSchema().getOpenAPIV3Schema().getProperties().get("spec");
    }

    private void assertRecursion(JSONSchemaProps spec, int level, String key) {
        Assertions.assertEquals("object", spec.getType());
        Assertions.assertTrue(level >= 0);

        if (level == 0) {
            Assertions.assertTrue(spec.getXKubernetesPreserveUnknownFields());
        } else {
            Assertions.assertNotNull(spec.getProperties());
            var spec2 = spec.getProperties().get(key);
            Assertions.assertNotNull(spec2);
            assertRecursion(spec2, level - 1, key);
        }
    }

    private void assertListRecursion(Object object, int level, String key) {
        Assertions.assertEquals("object", getFromMap(object, "type"));
        Assertions.assertTrue(level >= 0);
        if (level == 0) {
            Assertions.assertEquals(true, getFromMap(object, "x-kubernetes-preserve-unknown-fields"));
        } else {
            var props = getFromMap(object, "properties");
            var array = getFromMap(props, key);
            Assertions.assertEquals("array", getFromMap(array, "type"));
            var items = getFromMap(array, "items");
            assertListRecursion(items, level - 1, key);
        }
    }

    private Object getFromMap(Object map, String key) {
        Assertions.assertTrue(map instanceof Map);
        return ((Map<?, ?>) map).get(key);
    }

}
