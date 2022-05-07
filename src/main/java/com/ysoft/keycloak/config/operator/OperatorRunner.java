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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

@Service
public class OperatorRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(OperatorRunner.class);
    private final SchemaGenerator schemaGenerator;
    private final ObjectProvider<Operator> operatorProvider;

    public OperatorRunner(SchemaGenerator schemaGenerator, ObjectProvider<Operator> operatorProvider) {
        this.schemaGenerator = schemaGenerator;
        this.operatorProvider = operatorProvider;
    }

    @Override
    public void run(String... args) {
        var command = parseArgs(args);
        command.run();
    }

    private Runnable parseArgs(String... args) {
        String command = args.length == 0 ? "" : args[0];
        switch (command) {
            case "":
            case "operator":
                return new OperatorCommand();
            case "schema":
                return new SchemaGeneratorCommand(args);
            default:
                throw new IllegalArgumentException(command);
        }
    }

    private class SchemaGeneratorCommand implements Runnable {
        private final Path target;

        private SchemaGeneratorCommand(String... args) {
            target = args.length < 2 ? Paths.get("") : Paths.get(args[1]);
        }

        @Override
        public void run() {
            schemaGenerator.generateSchema(target);
        }
    }

    private class OperatorCommand implements Runnable {

        @Override
        public void run() {
            operatorProvider.getObject();
        }
    }
}
