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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jboss.logging.Logger;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.plugins.python.api.PythonCheck;
import org.sonar.plugins.python.api.PythonFile;
import org.sonar.plugins.python.api.PythonVisitorContext;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.python.parser.PythonParser;
import org.sonar.python.tree.PythonTreeMaker;

public class PythonScanner extends AbstractScanner {
    private static final Logger LOG = Logger.getLogger(PythonScanner.class);
    private static final String PYTHON_FILE_EXTENSION = ".py";
    private List<Project> projects = null;
    private List<PythonCheck> visitors = null;

    public PythonScanner() {
        LOG.info("Created Python scanner (*" + PYTHON_FILE_EXTENSION + ")");
    }

    @Override
    public void init(
            @Nonnull IMessageDispatcher iMessageDispatcher,
            @Nonnull File clonedProject,
            @Nonnull ScanRequest request)
            throws CancelScanException {
        this.iMessageDispatcher = iMessageDispatcher;
        this.baseDir = clonedProject;
        this.visitors = List.of(new PythonCryptoDetection(this.iMessageDispatcher, this.baseDir));

        if (request.subfolder() != null) {
            this.baseDir = new File(this.baseDir.getPath() + File.separator + request.subfolder());
        }

        iMessageDispatcher.sendLabelMessage("Indexing packages ...");
        LOG.info("Indexing python packages ...");
        this.projects = getPackages(baseDir, new ArrayList<>());
        LOG.info("Found " + projects.size() + " python packages");
    }

    @Override
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
            iMessageDispatcher.sendLabelMessage("Scanning package " + projectStr);
            LOG.info("Scanning package " + projectStr);
            for (InputFile inputFile : project.getInputFiles()) {
                scanFile(project.getIdentifier(), inputFile, visitors);
            }
            counter++;
        }
        scanResult.setBom(getFinalBom());
        scanResult.setDuration((System.currentTimeMillis() - scanTimeStart) / 1000);

        return scanResult;
    }

    @Nonnull
    private List<Project> getPackages(
            @Nonnull File rootDir, @Nonnull final List<Project> projects) {
        List<InputFile> files = getPythonFiles(rootDir, new ArrayList<>());
        if (!files.isEmpty()) {
            Project project = new Project(getProjectIdentifierFromPath(rootDir), files);
            projects.add(project);
        }

        File[] filesInDir = rootDir.listFiles();
        if (filesInDir != null) {
            for (File file : filesInDir) {
                if (file.isDirectory() && !".git".equals(file.getName())) {
                    LOG.trace("Extracting files from package: " + file.getPath());
                    getPackages(file, projects);
                }
            }
        }
        return projects;
    }

    private boolean excludeFromIndexing(@Nonnull File file) {
        return file.getPath().contains("test/");
    }

    @Nonnull
    private List<InputFile> getPythonFiles(
            @Nonnull File directory, @Nonnull final List<InputFile> inputFiles) {
        File[] filesInDir = directory.listFiles();
        if (filesInDir != null) {
            for (File file : filesInDir) {
                if (file.isFile()
                        && file.getName().endsWith(PYTHON_FILE_EXTENSION)
                        && !excludeFromIndexing(file)) {
                    LOG.trace("Found python file: " + file.getPath());
                    try {
                        TestInputFileBuilder builder = createTestFileBuilder(directory, file);
                        builder.setLanguage("python");
                        inputFiles.add(builder.build());
                    } catch (IOException iox) {
                        // ignore file
                    }
                }
            }
        }
        return inputFiles;
    }

    public static void scanFile(
            @Nonnull String packageName,
            @Nonnull InputFile file,
            @Nonnull List<PythonCheck> visitors) {
        PythonVisitorContext context =
                createContext(file, packageName.isEmpty() ? null : packageName);
        for (PythonCheck visitor : visitors) {
            visitor.scanFile(context);
        }
    }

    @Nonnull
    public static PythonVisitorContext createContext(
            @Nonnull InputFile file, @Nullable String packageName) {
        return createContext(file, null, packageName);
    }

    @Nonnull
    public static PythonVisitorContext createContext(
            @Nonnull InputFile file,
            @Nullable File workingDirectory,
            @Nullable String packageName) {
        TestPythonFile pythonFile = new TestPythonFile(file);
        FileInput rootTree = parseFile(pythonFile);
        return new PythonVisitorContext(rootTree, pythonFile, workingDirectory, packageName);
    }

    @Nonnull
    private static FileInput parseFile(@Nonnull TestPythonFile file) {
        var astNode = PythonParser.create().parse(file.content());
        return (new PythonTreeMaker()).fileInput(astNode);
    }

    private record TestPythonFile(@Nonnull InputFile file) implements PythonFile {
        @Override
        public String content() {
            try {
                return file.contents();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + file, e);
            }
        }

        @Override
        public String fileName() {
            return file.filename();
        }

        @Override
        public URI uri() {
            return file.uri();
        }

        @Override
        public String key() {
            return file.key();
        }
    }
}
