/*
 * CBOMkit
 * Copyright (C) 2025 IBM
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.usecases.scanning.services.pkg;

import com.github.packageurl.PackageURL;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PackageFinderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageFinderService.class);

    protected Path root;

    public PackageFinderService(@Nonnull Path root) throws IllegalArgumentException {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Path must be a directory!");
        }
        this.root = root;
    }

    @Nullable public Optional<Path> findPackage(@Nonnull PackageURL purl) throws IOException {
        List<Path> poms;

        LOGGER.info("Trying to find package path for purl {}", purl);

        try (Stream<Path> walk = Files.walk(this.root)) {
            poms =
                    walk.filter(p -> !Files.isDirectory(p))
                            // .map(p -> p.toString().toLowerCase())
                            // .filter(p -> p.endsWith(getBuildFileName()))
                            .filter(p -> isBuildFile(p))
                            .collect(Collectors.toList());
        }

        for (Path pom : poms) {
            try {
                if (purl.getName().equals(getPackageName(pom))) {
                    Path pkgPath = this.root.relativize(pom.getParent());
                    LOGGER.info(
                            "Package path: {}", pkgPath.equals(Paths.get("")) ? "<root>" : pkgPath);
                    return Optional.of(pkgPath);
                }
            } catch (Exception e) {
                continue;
            }
        }

        LOGGER.warn("Package path not found");
        return Optional.empty();
    }

    public abstract boolean isBuildFile(@Nonnull Path file);

    public abstract String getPackageName(@Nonnull Path buildFile) throws Exception;
}
