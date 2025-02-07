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

import jakarta.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

public class GradlePackageFinderService extends PackageFinderService {
    public GradlePackageFinderService(@Nonnull File rootFile) throws IllegalArgumentException {
        super(rootFile);
    }

    @Override
    public boolean isBuildFile(@Nonnull Path file) {
        return file.endsWith("build.gradle") || file.endsWith("build.gradle.kts");
    }

    @Override
    public Optional<String> getPackageName(@Nonnull Path buildFile) throws Exception {
        try (ProjectConnection connection =
                GradleConnector.newConnector()
                        .forProjectDirectory(buildFile.toFile().getParentFile())
                        .connect()) {

            Project project = connection.getModel(Project.class);
            PublishingExtension publishing =
                    project.getExtensions().findByType(PublishingExtension.class);

            if (publishing != null) {
                String projectName =
                        publishing.getPublications().withType(MavenPublication.class).stream()
                                .findFirst()
                                .map(MavenPublication::getArtifactId)
                                .orElse(project.getName());
                return Optional.ofNullable(projectName);
            }

            return Optional.ofNullable(project.getName());
        } catch (RuntimeException rte) {
            throw new Exception(rte);
        }
    }
}
