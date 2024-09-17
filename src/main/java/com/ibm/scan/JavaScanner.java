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
package com.ibm.scan;

import com.ibm.message.IMessageDispatcher;
import com.ibm.model.Project;
import com.ibm.model.api.ScanRequest;
import com.ibm.resources.v1.ScannerResource.CancelScanException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.java.SonarComponents;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;

public class JavaScanner extends AbstractScanner {
    private static final Logger LOG = Logger.getLogger(JavaScanner.class);
    private static final JavaVersion JAVA_VERSION =
            new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED);
    private static final String JAVA_FILE_EXTENSION = ".java";

    private List<Project> projects = null;
    private List<JavaCheck> visitors = null;
    private SonarComponents sonarComponents = null;

    private static final List<File> JARS =
            Collections.singletonList(
                    new File("src/main/resources/java/scan/bcprov-jdk18on-1.78.1.jar"));

    public JavaScanner() {
        LOG.info("Created Java scanner (*" + JAVA_FILE_EXTENSION + ")");
    }

    @SuppressWarnings("all")
    @Override
    public void init(
            @Nonnull IMessageDispatcher iMessageDispatcher,
            @Nonnull File clonedProject,
            @Nonnull ScanRequest request)
            throws CancelScanException {
        this.iMessageDispatcher = iMessageDispatcher;
        this.baseDir = clonedProject;
        this.visitors = List.of(new JavaCryptoDetection(this.iMessageDispatcher, this.baseDir));

        SensorContextTester sensorContext = SensorContextTester.create(this.baseDir);
        sensorContext.setSettings(
                new MapSettings()
                        .setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, false)
                        .setProperty(SonarComponents.SONAR_AUTOSCAN, false));
        if (request.subfolder() != null) {
            this.baseDir = new File(this.baseDir.getPath() + File.separator + request.subfolder());
        }

        DefaultFileSystem fileSystem = sensorContext.fileSystem();
        ClasspathForMain classpathForMain =
                new ClasspathForMain(sensorContext.config(), fileSystem);
        ClasspathForTest classpathForTest =
                new ClasspathForTest(sensorContext.config(), fileSystem);
        this.sonarComponents =
                new SonarComponents(
                        null, fileSystem, classpathForMain, classpathForTest, null, null);
        this.sonarComponents.setSensorContext(sensorContext);

        iMessageDispatcher.sendLabelMessage("Indexing projects ...");
        LOG.info("Indexing java projects ...");
        this.projects = getProjects(baseDir, new ArrayList<>());
        LOG.info("Found " + projects.size() + " java projects");
    }

    @Override
    @Nonnull
    public IScanner.ScanResult scan() throws CancelScanException {
        final IScanner.ScanResult scanResult = new IScanner.ScanResult();
        if (visitors == null || projects == null) {
            LOG.error("Scanner not initialized");
            return scanResult;
        }

        long scanTimeStart = System.currentTimeMillis();
        int counter = 1;
        for (Project project : projects) {
            scanResult.addNumFiles(project.getInputFiles().size());
            scanResult.addNumLines(
                    project.getInputFiles().stream().map(InputFile::lines).reduce(0, Integer::sum));

            String projectStr =
                    project.getIdentifier() + " (" + counter + "/" + projects.size() + ")";
            iMessageDispatcher.sendLabelMessage("Scanning project " + projectStr);
            LOG.info("Scanning project " + projectStr);
            JavaAstScannerExtension jscanner =
                    new JavaAstScannerExtension(sonarComponents, iMessageDispatcher, projectStr);
            // add bc to classpath to resolve types
            VisitorsBridge visitorBridge =
                    new VisitorsBridge(visitors, JARS, sonarComponents, JAVA_VERSION);
            jscanner.setVisitorBridge(visitorBridge);
            jscanner.scan(project.getInputFiles());
            counter++;
        }
        scanResult.setBom(getFinalBom());
        scanResult.setDuration((System.currentTimeMillis() - scanTimeStart) / 1000);
        return scanResult;
    }

    @Nonnull
    private List<Project> getProjects(
            @Nonnull File rootDir, @Nonnull final List<Project> projects) {
        File[] filesInDir = rootDir.listFiles();
        if (filesInDir == null) {
            return Collections.emptyList();
        }
        boolean hasPomFile = containsPOMorGRADLEfile(filesInDir);
        for (File file : filesInDir) {
            if (hasPomFile) {
                if (file.isDirectory() && !".git".equals(file.getName())) {
                    LOG.trace("Extracting files from project: " + file.getPath());
                    getProjects(file, projects);
                }
            } else {
                List<InputFile> files = getJavaFiles(file, new ArrayList<>());
                if (!files.isEmpty()) {
                    Project project = new Project(getProjectIdentifierFromPath(file), files);
                    projects.add(project);
                }
            }
        }
        return projects;
    }

    private boolean excludeFromIndexing(@Nonnull File file) {
        return file.getPath().contains("src/test/") || file.getName().contains("package-info");
    }

    @Nonnull
    private List<InputFile> getJavaFiles(
            @Nonnull File directory, @Nonnull final List<InputFile> inputFiles) {
        File[] filesInDir = directory.listFiles();
        if (filesInDir != null) {
            for (File file : filesInDir) {
                if (file.isDirectory() && !".git".equals(file.getName())) {
                    getJavaFiles(new File(directory + File.separator + file.getName()), inputFiles);
                } else if (file.isFile()
                        && file.getName().endsWith(JAVA_FILE_EXTENSION)
                        && !excludeFromIndexing(file)) {
                    LOG.trace("Found java file: " + file.getPath());
                    try {
                        TestInputFileBuilder builder = createTestFileBuilder(directory, file);
                        builder.setLanguage("java");
                        inputFiles.add(builder.build());
                    } catch (IOException iox) {
                        // ignore file
                    }
                }
            }
        }
        return inputFiles;
    }

    private boolean containsPOMorGRADLEfile(@Nonnull File[] files) {
        return Arrays.stream(files)
                .anyMatch(f -> f.getName().equals("pom.xml") || f.getName().equals("build.gradle"));
    }
}
