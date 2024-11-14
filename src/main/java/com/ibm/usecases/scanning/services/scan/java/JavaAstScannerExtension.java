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

import com.ibm.infrastructure.progress.IProgressDispatcher;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jboss.logging.Logger;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisException;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaVersion;

public class JavaAstScannerExtension extends JavaAstScanner {
    private static final Logger LOG = Logger.getLogger(JavaAstScannerExtension.class);

    private static final String LOG_ERROR_STACKOVERFLOW =
            "A stack overflow error occurred while analyzing file: '%s'";
    private static final String LOG_ERROR_UNABLE_TO_PARSE_FILE =
            "Unable to parse source file : '%s'";
    private static final String LOG_WARN_MISCONFIGURED_JAVA_VERSION =
            "Analyzing '%s' file with misconfigured Java version."
                    + " Please check that property '%s' is correctly configured (currently set to: %d) or exclude 'module-info.java' files from analysis."
                    + " Such files only exist in Java9+ projects.";

    private final SonarComponents sonarComponents;
    private VisitorsBridge visitor;

    private final IProgressDispatcher progressDispatcher;
    private boolean reportedMisconfiguredVersion = false;

    private int currentFileNumber = 0;
    private long filesSize = 0;
    private final String projectIdentifier;

    public JavaAstScannerExtension(
            @Nullable SonarComponents sonarComponents,
            @Nonnull IProgressDispatcher progressDispatcher,
            @Nonnull String projectIdentifier) {
        super(sonarComponents);
        this.sonarComponents = sonarComponents;
        this.projectIdentifier = projectIdentifier;
        this.progressDispatcher = progressDispatcher;
    }

    @Override
    public List<File> getClasspath() {
        return visitor.getClasspath();
    }

    /**
     * Attempt to scan files without parsing, using the raw input file and cached information.
     *
     * @param inputFiles The list of files to analyze
     * @return A map with 2 lists of inputFiles. Under the {@code true} key, files that have
     *     successfully been scanned without parsing and, under the {@code false} key, files that
     *     need to be parsed for further analysis.
     */
    @Override
    public Map<Boolean, List<InputFile>> scanWithoutParsing(
            @Nonnull Iterable<? extends InputFile> inputFiles) {
        return StreamSupport.stream(inputFiles.spliterator(), false)
                // Split files between successfully scanned without parsing and failed to scan
                // without parsing
                .collect(Collectors.partitioningBy(visitor::scanWithoutParsing));
    }

    @Override
    public void scan(@Nonnull Iterable<? extends InputFile> inputFiles) {
        this.filesSize = StreamSupport.stream(inputFiles.spliterator(), false).count();
        List<InputFile> filesNames = filterModuleInfo(inputFiles).collect(Collectors.toList());
        AnalysisProgress analysisProgress = new AnalysisProgress(filesNames.size());
        try {
            boolean shouldIgnoreUnnamedModuleForSplitPacakge =
                    sonarComponents != null
                            && sonarComponents.shouldIgnoreUnnamedModuleForSplitPackage();
            JParserConfig.Mode.FILE_BY_FILE
                    .create(
                            visitor.getJavaVersion(),
                            visitor.getClasspath(),
                            shouldIgnoreUnnamedModuleForSplitPacakge)
                    .parse(
                            filesNames,
                            this::refinedAnalysisCancelled,
                            analysisProgress,
                            (i, r) ->
                                    simpleScan(
                                            i,
                                            r,
                                            // Due to a bug in ECJ, JAR files remain locked after
                                            // the analysis on Windows,
                                            // we unlock them manually. See SONARJAVA-3609.
                                            JavaAstScannerExtension::cleanUpAst));
        } finally {
            endOfAnalysis();
        }
    }

    @Override
    public <T extends InputFile> Stream<T> filterModuleInfo(@Nonnull Iterable<T> inputFiles) {
        JavaVersion javaVersion = visitor.getJavaVersion();
        return StreamSupport.stream(inputFiles.spliterator(), false)
                .filter(
                        file -> {
                            if (("module-info.java".equals(file.filename()))
                                    && !javaVersion.isNotSet()
                                    && javaVersion.asInt() <= 8) {
                                // When the java version is not set, we use the maximum version
                                // supported, able
                                // to parse module info.
                                refinedLogMisconfiguredVersion(javaVersion);
                                return false;
                            }
                            return true;
                        });
    }

    @Override
    public void endOfAnalysis() {
        visitor.endOfAnalysis();
        refinedLogUndefinedTypes();
    }

    private void refinedLogUndefinedTypes() {
        if (sonarComponents != null) {
            sonarComponents.logUndefinedTypes();
        }
    }

    private boolean refinedAnalysisCancelled() {
        return sonarComponents != null && sonarComponents.analysisCancelled();
    }

    @Override
    public void simpleScan(
            @Nonnull InputFile inputFile,
            @Nonnull JParserConfig.Result result,
            @Nonnull Consumer<JavaTree.CompilationUnitTreeImpl> cleanUp) {

        try {
            this.currentFileNumber++;
            progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.LABEL,
                            "Scan project "
                                    + this.projectIdentifier
                                    + " with files "
                                    + this.currentFileNumber
                                    + "/"
                                    + this.filesSize));
            visitor.setCurrentFile(inputFile);
            JavaTree.CompilationUnitTreeImpl ast = result.get();
            visitor.visitFile(
                    ast, sonarComponents != null && sonarComponents.fileCanBeSkipped(inputFile));
            String path = inputFile.toString();
            refinedCollectUndefinedTypes(path, ast.sema.undefinedTypes());
            cleanUp.accept(ast);
        } catch (RecognitionException e) {
            checkInterrupted(e);
            LOG.error(String.format(LOG_ERROR_UNABLE_TO_PARSE_FILE, inputFile));
            LOG.error(e.getMessage());

            refinedParseErrorWalkAndVisit(e, inputFile);
        } catch (AnalysisException e) {
            throw e;
        } catch (Exception e) {
            checkInterrupted(e);
            refinedInterruptIfFailFast(e, inputFile);
        } catch (StackOverflowError error) {
            LOG.error(String.format(LOG_ERROR_STACKOVERFLOW, inputFile), error);
            throw error;
        }
    }

    private static void cleanUpAst(@Nonnull JavaTree.CompilationUnitTreeImpl ast) {
        // release environment used for semantic resolution
        ast.sema.getEnvironmentCleaner().run();
    }

    private void refinedCollectUndefinedTypes(String path, Set<JProblem> undefinedTypes) {
        if (sonarComponents != null) {
            sonarComponents.collectUndefinedTypes(path, undefinedTypes);
        }
    }

    void refinedLogMisconfiguredVersion(JavaVersion javaVersion) {
        if (!reportedMisconfiguredVersion) {
            LOG.warn(
                    String.format(
                            LOG_WARN_MISCONFIGURED_JAVA_VERSION,
                            "module-info.java",
                            JavaVersion.SOURCE_VERSION,
                            javaVersion.asInt()));
            reportedMisconfiguredVersion = true;
        }
    }

    private void refinedInterruptIfFailFast(Exception e, InputFile inputFile) {
        if (shouldFailAnalysis()) {
            throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e);
        }
    }

    @Override
    public boolean shouldFailAnalysis() {
        return sonarComponents != null && sonarComponents.shouldFailAnalysisOnException();
    }

    @Override
    public void checkInterrupted(@Nonnull Exception e) {
        Throwable cause = ExceptionUtils.getRootCause(e);
        if (cause instanceof InterruptedException
                || cause instanceof InterruptedIOException
                || cause instanceof CancellationException
                || refinedAnalysisCancelled()) {
            throw new AnalysisException("Analysis cancelled", e);
        }
    }

    private void refinedParseErrorWalkAndVisit(RecognitionException e, InputFile inputFile) {
        try {
            visitor.processRecognitionException(e, inputFile);
        } catch (Exception e2) {
            throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e2);
        }
    }

    private static String getAnalysisExceptionMessage(InputFile file) {
        return String.format("Unable to analyze file : '%s'", file);
    }

    @Override
    public void setVisitorBridge(@Nonnull VisitorsBridge visitor) {
        this.visitor = visitor;
    }
}
