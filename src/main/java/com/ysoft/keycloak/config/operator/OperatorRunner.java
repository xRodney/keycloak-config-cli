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

package com.ysoft.keycloak.config.operator;

import com.ysoft.keycloak.config.operator.schema.SchemaGenerator;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.OperatorException;
import io.javaoperatorsdk.operator.api.config.ExecutorServiceManager;
import io.javaoperatorsdk.operator.processing.LifecycleAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class OperatorRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(OperatorRunner.class);
    private final SchemaGenerator schemaGenerator;
    private final Operator operator;

    public OperatorRunner(SchemaGenerator schemaGenerator, Operator operator) {
        this.schemaGenerator = schemaGenerator;
        this.operator = operator;
    }

    @Override
    public void run(String... args) {
//        log.info("Generating schema");
//        schemaGenerator.generateSchema(Paths.get(""));
//        operator = operatorProvider.getObject();
//        operator.start();
//        runUntilTerminated();
    }
}
