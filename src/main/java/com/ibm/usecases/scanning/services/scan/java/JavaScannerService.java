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
package com.ibm.usecases.scanning.services.scan.java;

import com.ibm.domain.scanning.CBOM;
import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.Revision;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import com.ibm.usecases.scanning.services.indexing.ProjectModule;
import com.ibm.usecases.scanning.services.scan.ScanResultDTO;
import com.ibm.usecases.scanning.services.scan.ScannerService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaFrontend;
import org.sonar.java.SonarComponents;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;

public final class JavaScannerService extends ScannerService {
    private static final JavaVersion JAVA_VERSION =
            new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED);

    @Nonnull private final String getJavaDependencyJARSPath;

    public JavaScannerService(
            @Nonnull IProgressDispatcher progressDispatcher,
            @Nonnull String getJavaDependencyJARSPath,
            @Nonnull File projectDirectory) {
        super(progressDispatcher, projectDirectory);
        this.getJavaDependencyJARSPath = getJavaDependencyJARSPath;
    }

    @Override
    @Nonnull
    public synchronized ScanResultDTO scan(
            @Nonnull GitUrl gitUrl,
            @Nonnull Revision revision,
            @Nonnull Commit commit,
            @Nullable Path packageFolder,
            @Nonnull List<ProjectModule> index)
            throws ClientDisconnected {
        final File targetJarClasses = new File(this.projectDirectory, "target/classes");
        if (!targetJarClasses.exists()) {
            this.progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.WARNING,
                            "No target folder found in java project. This reduces the accuracy of the findings."));
        }

        final SensorContextTester sensorContext = SensorContextTester.create(this.projectDirectory);
        sensorContext.setSettings(
                new MapSettings()
                        .setProperty(SonarComponents.SONAR_BATCH_MODE_KEY, true)
                        .setProperty("sonar.java.libraries", this.getJavaDependencyJARSPath)
                        .setProperty(
                                "sonar.java.binaries",
                                new File(this.projectDirectory, "target/classes").toString())
                        .setProperty(SonarComponents.SONAR_AUTOSCAN, false)
                        .setProperty(SonarComponents.SONAR_BATCH_SIZE_KEY, 8 * 1024 * 1024));
        final DefaultFileSystem fileSystem = sensorContext.fileSystem();
        final ClasspathForMain classpathForMain =
                new ClasspathForMain(sensorContext.config(), fileSystem);
        final ClasspathForTest classpathForTest =
                new ClasspathForTest(sensorContext.config(), fileSystem);
        final SonarComponents sonarComponents =
                getSonarComponents(fileSystem, classpathForMain, classpathForTest);
        sonarComponents.setSensorContext(sensorContext);
        LOGGER.info("Start scanning {} java projects", index.size());

        final JavaResourceLocator javaResourceLocator =
                new DefaultJavaResourceLocator(classpathForMain, classpathForTest);
        final JavaFrontend javaFrontend =
                new JavaFrontend(
                        JAVA_VERSION,
                        sonarComponents,
                        null,
                        javaResourceLocator,
                        null,
                        new JavaDetectionCollectionRule(this));

        long scanTimeStart = System.currentTimeMillis();
        int counter = 1;
        int numberOfScannedLines = 0;
        int numberOfScannedFiles = 0;
        for (ProjectModule project : index) {
            numberOfScannedFiles += project.inputFileList().size();
            numberOfScannedLines +=
                    project.inputFileList().stream().map(InputFile::lines).reduce(0, Integer::sum);

            final String projectStr =
                    project.identifier() + " (" + counter + "/" + index.size() + ")";
            this.progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.LABEL, "Scanning project " + projectStr));

            javaFrontend.scan(project.inputFileList(), List.of(), List.of());
            counter++;
        }

        return new ScanResultDTO(
                scanTimeStart,
                System.currentTimeMillis(),
                numberOfScannedLines,
                numberOfScannedFiles,
                this.receiveBom(projectDirectory, gitUrl, revision, commit, packageFolder)
                        .map(CBOM::new)
                        .orElse(null));
    }

    @Nonnull
    private static SonarComponents getSonarComponents(
            DefaultFileSystem fileSystem,
            ClasspathForMain classpathForMain,
            ClasspathForTest classpathForTest) {
        final FileLinesContextFactory fileLinesContextFactory =
                inputFile ->
                        new FileLinesContext() {
                            @Override
                            public void setIntValue(@Nonnull String s, int i, int i1) {
                                // nothing
                            }

                            @Override
                            public void setStringValue(
                                    @Nonnull String s, int i, @Nonnull String s1) {
                                // nothing
                            }

                            @Override
                            public void save() {
                                // nothing
                            }
                        };
        return new SonarComponents(
                fileLinesContextFactory,
                fileSystem,
                classpathForMain,
                classpathForTest,
                null,
                null);
    }
}
