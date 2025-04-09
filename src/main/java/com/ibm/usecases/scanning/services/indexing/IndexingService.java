/*
 * CBOMkit
 * Copyright (C) 2024 IBM
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
package com.ibm.usecases.scanning.services.indexing;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

public abstract class IndexingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingService.class);

    @Nonnull private final IProgressDispatcher progressDispatcher;
    private final String languageIdentifier;
    private final String languageFileExtension;
    @Nonnull private File baseDirectory;
    @Nullable private IBuildType mainBuildType;

    protected IndexingService(
            @Nonnull IProgressDispatcher progressDispatcher,
            @Nonnull File baseDirectory,
            @Nonnull String languageIdentifier,
            @Nonnull String languageFileExtension) {
        this.progressDispatcher = progressDispatcher;
        this.baseDirectory = baseDirectory;
        this.languageIdentifier = languageIdentifier;
        this.languageFileExtension = languageFileExtension;
    }

    @Nonnull
    public List<ProjectModule> index(@Nullable Path packageFolder) throws ClientDisconnected {
        Optional.ofNullable(packageFolder)
                .ifPresent(path -> baseDirectory = baseDirectory.toPath().resolve(path).toFile());
        this.progressDispatcher.send(
                new ProgressMessage(ProgressMessageType.LABEL, "Indexing projects ..."));
        final List<ProjectModule> projectModules = new ArrayList<>();
        detectModules(baseDirectory, projectModules);
        return projectModules;
    }

    private void detectModules(
            @Nonnull File projectDirectory, @Nonnull List<ProjectModule> projectModules) {
        if (projectDirectory.isFile()) {
            return;
        }
        if (isModule(projectDirectory)) {
            // Contains build files that indicates that this should be indexed as a module.
            // This module cannot be composed of more modules
            if (this.mainBuildType == null) {
                this.mainBuildType = this.getMainBuildTypeFromModuleDirectory(projectDirectory);
            }
            addProjectModuleFromDirectory(projectModules, projectDirectory);
        } else {
            // this directory is not a module
            final File[] filesInDir = projectDirectory.listFiles();
            if (filesInDir == null) {
                return;
            }
            for (File file : filesInDir) {
                if (file.isDirectory() && !file.getName().equals(".git")) {
                    this.detectModules(file, projectModules);
                }
            }
            // if no models where found just add all files
            if (projectModules.isEmpty()) {
                addProjectModuleFromDirectory(projectModules, projectDirectory);
            }
        }
    }

    void addProjectModuleFromDirectory(
            @Nonnull List<ProjectModule> projectModules, @Nonnull File projectDirectory) {
        final String projectIdentifier = getProjectIdentifier(projectDirectory);
        final File[] filesInDirectory = projectDirectory.listFiles();
        final List<InputFile> files = new ArrayList<>();
        collectInputFiles(filesInDirectory, projectDirectory, projectModules, files);

        if (!files.isEmpty()) {
            LOGGER.info(
                    "Created project module '{}' [{} {} files]",
                    projectIdentifier,
                    files.size(),
                    this.languageFileExtension);
            projectModules.add(new ProjectModule(projectIdentifier, files));
        }
    }

    void collectInputFiles(
            @Nullable File[] fileList,
            @Nonnull File projectDirectory,
            @Nonnull List<ProjectModule> projectModules,
            @Nonnull final List<InputFile> inputFiles) {
        if (fileList == null) {
            return;
        }
        for (File file : fileList) {
            if (file.isDirectory() && !file.getName().equals(".git")) {
                if (isModule(file)) {
                    addProjectModuleFromDirectory(projectModules, file);
                } else {
                    collectInputFiles(
                            file.listFiles(), projectDirectory, projectModules, inputFiles);
                }
                continue;
            }
            // apply filter
            if (!this.excludeFromIndexing(file)
                    && file.getName().endsWith(this.languageFileExtension)) {
                try {
                    final TestInputFileBuilder builder =
                            createTestFileBuilder(projectDirectory, file);
                    builder.setLanguage(this.languageIdentifier);
                    inputFiles.add(builder.build());
                } catch (IOException iox) {
                    LOGGER.debug(iox.getLocalizedMessage());
                }
            }
        }
    }

    @Nonnull
    protected TestInputFileBuilder createTestFileBuilder(
            @Nonnull File projectDirectory, @Nonnull File file) throws IOException {
        Charset encoding = null;
        String contents = null;
        for (Charset cs : List.of(UTF_8, ISO_8859_1)) {
            try {
                contents = Files.readString(file.toPath(), cs);
                encoding = cs;
                break;
            } catch (Exception error) {
                LOGGER.error("Error reading file {}: {}", file.getPath(), error.getMessage());
            }
        }
        if (contents == null || encoding == null) {
            throw new IOException("Invalid encoding of file " + file);
        }
        return new TestInputFileBuilder("", projectDirectory, file)
                .setProjectBaseDir(projectDirectory.toPath())
                .setContents(contents)
                .setCharset(encoding)
                .setType(InputFile.Type.MAIN);
    }

    @Nonnull
    public Optional<IBuildType> getMainBuildType() {
        return Optional.ofNullable(mainBuildType);
    }

    @Nonnull
    protected String getProjectIdentifier(@Nonnull File directory) {
        return baseDirectory.toPath().relativize(directory.toPath()).toString();
    }

    abstract boolean isModule(@Nonnull File directory);

    @Nullable abstract IBuildType getMainBuildTypeFromModuleDirectory(@Nonnull File directory);

    abstract boolean excludeFromIndexing(@Nonnull File file);
}
