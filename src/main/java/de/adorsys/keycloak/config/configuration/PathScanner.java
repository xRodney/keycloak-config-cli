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

import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import org.apache.commons.io.file.AccumulatorPathVisitor;
import org.apache.commons.io.file.PathFilter;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PathScanner {
    public static final String FILE = "file:";

    private static WildcardFileFilter DOT_HIDDEN = new WildcardFileFilter(".*");

    private final ImportConfigProperties importConfigProperties;

    @Inject
    public PathScanner(ImportConfigProperties importConfigProperties) {
        this.importConfigProperties = importConfigProperties;
    }

    public List<Resource> getResources(String location) throws IOException, URISyntaxException {

        PathFilter fileFilter = getFileFilter();
        PathFilter dirFilter = getDirectoryFilter();

        return getFileResources(location, fileFilter, dirFilter);
    }

    private PathFilter getFileFilter() {
        var config = importConfigProperties.getFiles();

        IOFileFilter filter = new WildcardFileFilter(config.getPattern());
        if (!config.isIncludeHiddenFiles()) {
            filter = filter.and(HiddenFileFilter.VISIBLE).and(DOT_HIDDEN.negate());
        }
        if (!config.getExcludes().isEmpty()) {
            filter = filter.and(new WildcardFileFilter(config.getExcludes()).negate());
        }
        return filter;
    }

    private PathFilter getDirectoryFilter() {
        var config = importConfigProperties.getFiles();

        if (config.isIncludeHiddenFiles()) {
            return TrueFileFilter.TRUE;
        } else {
            return HiddenFileFilter.VISIBLE.and(DOT_HIDDEN.negate());
        }
    }

    private List<Resource> getFileResources(String location, PathFilter fileFilter, PathFilter dirFilter) throws IOException {
        var file = new File(StringUtils.stripStart(location, FILE));
        if (file.isDirectory()) {
            final AccumulatorPathVisitor visitor = AccumulatorPathVisitor.withLongCounters(fileFilter, dirFilter);
            Files.walkFileTree(file.toPath(), visitor);

            return visitor.getFileList().stream()
                    .map(Path::toFile)
                    .map(FileResource::new)
                    .collect(Collectors.toList());
        }

        if (file.exists()) {
            return List.of(new FileResource(file));
        }
        return List.of();
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
