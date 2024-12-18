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
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.java.SonarComponents;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;

public final class JavaScannerService extends ScannerService {
    private static final JavaVersion JAVA_VERSION =
            new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED);

    @Nonnull private final List<File> getJavaDependencyJARS;

    public JavaScannerService(
            @Nonnull IProgressDispatcher progressDispatcher,
            @Nonnull List<File> getJavaDependencyJARS,
            @Nonnull File projectDirectory) {
        super(progressDispatcher, projectDirectory);
        this.getJavaDependencyJARS = getJavaDependencyJARS;
    }

    @Override
    @Nonnull
    public synchronized ScanResultDTO scan(
            @Nonnull GitUrl gitUrl,
            @Nonnull Revision revision,
            @Nonnull Commit commit,
            @Nullable String subFolder,
            @Nonnull List<ProjectModule> index)
            throws ClientDisconnected {
        final List<JavaCheck> visitors = List.of(new JavaDetectionCollectionRule(this));
        final SensorContextTester sensorContext = SensorContextTester.create(projectDirectory);
        sensorContext.setSettings(
                new MapSettings()
                        .setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, false)
                        .setProperty(SonarComponents.SONAR_AUTOSCAN, false));
        final DefaultFileSystem fileSystem = sensorContext.fileSystem();
        final ClasspathForMain classpathForMain =
                new ClasspathForMain(sensorContext.config(), fileSystem);
        final ClasspathForTest classpathForTest =
                new ClasspathForTest(sensorContext.config(), fileSystem);
        final SonarComponents sonarComponents =
                new SonarComponents(
                        null, fileSystem, classpathForMain, classpathForTest, null, null);
        sonarComponents.setSensorContext(sensorContext);

        LOGGER.info("Start scanning {} java projects", index.size());

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

            final JavaAstScannerExtension jscanner =
                    new JavaAstScannerExtension(
                            sonarComponents, this.progressDispatcher, projectStr);
            VisitorsBridge visitorBridge =
                    new VisitorsBridge(
                            visitors, this.getJavaDependencyJARS, sonarComponents, JAVA_VERSION);
            jscanner.setVisitorBridge(visitorBridge);
            jscanner.scan(project.inputFileList());
            counter++;
        }

        return new ScanResultDTO(
                scanTimeStart,
                System.currentTimeMillis(),
                numberOfScannedLines,
                numberOfScannedFiles,
                this.receiveBom(projectDirectory, gitUrl, revision, commit, subFolder)
                        .map(CBOM::new)
                        .orElse(null));
    }
}
