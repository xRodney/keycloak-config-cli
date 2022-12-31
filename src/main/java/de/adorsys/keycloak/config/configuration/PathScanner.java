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

package de.adorsys.keycloak.config.configuration;

import de.adorsys.keycloak.config.exception.InvalidImportException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class PathScanner {
    public static final String FILE = "file:";

    public List<Resource> getResources(String location) {
        var file = new File(StringUtils.stripStart(location, FILE));
        if (file.isDirectory()) {
            throw new InvalidImportException("Unable to load " + location + ". It is a directory");
        }
        return file.exists() ? List.of(new FileResource(file)) : List.of();
    }

    private static class FileResource extends AbstractResource {

        private final File file;

        private FileResource(File file) {
            this.file = file;
        }

        @Override
        public String getDescription() {
            return file.getPath();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(getFile());
        }

        @Override
        public File getFile() {
            return file;
        }

        @Override
        public boolean isFile() {
            return true;
        }

        @Override
        public URI getURI() {
            return file.toURI();
        }
    }
}
