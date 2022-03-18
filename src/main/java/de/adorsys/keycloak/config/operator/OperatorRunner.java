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

import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ExecutorServiceManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Not sure why ths is needed, but without this the operator never starts and the app just exits
 */
@Service
public class OperatorRunner implements CommandLineRunner {
    private final Operator operator;

    public OperatorRunner(Operator operator) {
        this.operator = operator;
    }

    @Override
    public void run(String... args) throws Exception {
        ExecutorServiceManager.instance().executorService().awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}
