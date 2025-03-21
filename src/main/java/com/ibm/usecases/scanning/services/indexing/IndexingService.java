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
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    public class IndexFileFilter implements FileFilter {
        private final String extension;

        public IndexFileFilter(String extension) {
            this.extension = extension;
        }

        @Override
        public boolean accept(File f) {
            return !excludeFromIndexing(f)
                    && ((f.isDirectory() && !".git".equals(f.getName()))
                            || (f.isFile() && f.getName().endsWith(this.extension)));
        }
    }

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
    public List<ProjectModule> index(Optional<Path> packageFolder) throws ClientDisconnected {
        packageFolder.ifPresent(
                path -> baseDirectory = baseDirectory.toPath().resolve(path).toFile());
        this.progressDispatcher.send(
                new ProgressMessage(ProgressMessageType.LABEL, "Indexing projects ..."));
        return detectModules(baseDirectory, new ArrayList<>());
    }

    private List<ProjectModule> detectModules(
            @Nonnull File projectDirectory, @Nonnull List<ProjectModule> projectModules) {
        if (isModule(projectDirectory)) {
            final File[] filesInDir =
                    projectDirectory.listFiles(new IndexFileFilter(this.languageFileExtension));
            if (filesInDir == null) {
                return Collections.emptyList();
            }

            LOGGER.debug("Extracting projects from module: {}", projectDirectory);
            Arrays.sort(filesInDir);
            for (File file : filesInDir) {
                if (file.isDirectory()) {
                    this.detectModules(file, projectModules);
                }
            }
        } else {
            List<InputFile> files = getFiles(projectDirectory, projectModules, new ArrayList<>());
            if (!files.isEmpty()) {
                String projectIdentifier = getProjectIdentifier(projectDirectory);
                LOGGER.info(
                        "Created project module '{}' [{} {} files]",
                        projectIdentifier,
                        files.size(),
                        languageFileExtension);
                ProjectModule project = new ProjectModule(projectIdentifier, files);
                projectModules.add(project);
            }
        }
        return projectModules;
    }

    @Nonnull
    public List<InputFile> getFiles(
            @Nonnull File directory,
            @Nonnull List<ProjectModule> projectModules,
            @Nonnull final List<InputFile> inputFiles) {
        File[] filesInDir = directory.listFiles(new IndexFileFilter(this.languageFileExtension));
        if (filesInDir == null) {
            return Collections.emptyList();
        }

        if (isModule(directory)) {
            LOGGER.debug("Extracting projects from module: {}", directory);
            for (File file : filesInDir) {
                if (file.isDirectory()) {
                    this.detectModules(file, projectModules);
                }
            }
            return Collections.emptyList();
        }

        LOGGER.debug("Extracting files from directory: {}", directory);
        Arrays.sort(filesInDir);
        for (File file : filesInDir) {
            if (file.isDirectory()) {
                getFiles(
                        new File(directory + File.separator + file.getName()),
                        projectModules,
                        inputFiles);
            } else {
                LOGGER.debug("Found file: {}", file);
                try {
                    TestInputFileBuilder builder = createTestFileBuilder(directory, file);
                    builder.setLanguage(this.languageIdentifier);
                    inputFiles.add(builder.build());
                } catch (IOException iox) {
                    // ignore file
                }
            }
        }
        return inputFiles;
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
        if (contents == null) {
            throw new IOException("Invalid encoding of file " + file);
        }

        return new TestInputFileBuilder("", file.getPath())
                .setProjectBaseDir(projectDirectory.toPath())
                .setContents(contents)
                .setCharset(encoding)
                .setType(InputFile.Type.MAIN);
    }

    @Nonnull
    protected String getProjectIdentifier(@Nonnull File directory) {
        return baseDirectory.toPath().relativize(directory.toPath()).toString();
    }

    abstract boolean isModule(@Nonnull File directory);

    abstract boolean excludeFromIndexing(@Nonnull File file);
}
