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

package de.adorsys.keycloak.config.operator;

import de.adorsys.keycloak.config.operator.schema.SchemaGenerator;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.OperatorException;
import io.javaoperatorsdk.operator.api.config.ExecutorServiceManager;
import io.javaoperatorsdk.operator.processing.LifecycleAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Not sure why ths is needed, but without this the operator never starts and the app just exits.
 * <p>
 * So we use it for schema generation as well.
 */
@Service
public class OperatorRunner implements CommandLineRunner, LifecycleAware {
    private static final Logger log = LoggerFactory.getLogger(OperatorRunner.class);
    private final ObjectProvider<Operator> operatorProvider;
    private final SchemaGenerator schemaGenerator;
    private Operator operator;

    public OperatorRunner(ObjectProvider<Operator> operatorProvider, SchemaGenerator schemaGenerator) {
        this.operatorProvider = operatorProvider;
        this.schemaGenerator = schemaGenerator;
    }

    @Override
    public void run(String... args) {
        log.info("Generating schema");
        schemaGenerator.generateSchema(Paths.get(""));
        operator = operatorProvider.getObject();
        operator.start();
        runUntilTerminated();
    }

    private void runUntilTerminated() {
        boolean terminated = false;

        ExecutorService executorService = ExecutorServiceManager.instance().executorService();
        while (!terminated) {
            log.info("Operating...");
            try {
                terminated = executorService.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                log.info("Interrupted");
            }
        }
    }

    @Override
    public void start() throws OperatorException {

    }

    @Override
    public void stop() throws OperatorException {
        if (operator != null) {
            operator.stop();
        }

    }
}
