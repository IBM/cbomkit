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
package com.ibm.usecases.scanning.processmanager;

import app.bootstrap.core.cqrs.ICommand;
import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.cqrs.ProcessManager;
import app.bootstrap.core.ddd.IRepository;
import com.github.packageurl.PackageURL;
import com.ibm.domain.scanning.CBOM;
import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.Language;
import com.ibm.domain.scanning.LanguageScan;
import com.ibm.domain.scanning.ScanAggregate;
import com.ibm.domain.scanning.ScanId;
import com.ibm.domain.scanning.ScanMetadata;
import com.ibm.domain.scanning.ScanRequest;
import com.ibm.domain.scanning.errors.CBOMSerializationFailed;
import com.ibm.domain.scanning.errors.CommitHashAlreadyExists;
import com.ibm.domain.scanning.errors.GitUrlAlreadyResolved;
import com.ibm.domain.scanning.errors.ScanResultForLanguageAlreadyExists;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.errors.EntityNotFoundById;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import com.ibm.infrastructure.scanning.IScanConfiguration;
import com.ibm.usecases.scanning.commands.CloneGitRepositoryCommand;
import com.ibm.usecases.scanning.commands.FetchDataFromDepsDevCommand;
import com.ibm.usecases.scanning.commands.IndexModulesCommand;
import com.ibm.usecases.scanning.commands.RequestScanCommand;
import com.ibm.usecases.scanning.commands.ScanCommand;
import com.ibm.usecases.scanning.errors.GitCloneFailed;
import com.ibm.usecases.scanning.errors.GitCloneResultNotAvailable;
import com.ibm.usecases.scanning.errors.NoCommitProvided;
import com.ibm.usecases.scanning.errors.NoDataAvailableInDepsDevForPurl;
import com.ibm.usecases.scanning.errors.NoGitUrlSpecifiedForScan;
import com.ibm.usecases.scanning.errors.NoIndexForProject;
import com.ibm.usecases.scanning.errors.NoProjectDirectoryProvided;
import com.ibm.usecases.scanning.errors.NoPurlSpecifiedForScan;
import com.ibm.usecases.scanning.services.depsdev.DepsDevService;
import com.ibm.usecases.scanning.services.git.CloneResultDTO;
import com.ibm.usecases.scanning.services.git.GitService;
import com.ibm.usecases.scanning.services.indexing.JavaIndexService;
import com.ibm.usecases.scanning.services.indexing.ProjectModule;
import com.ibm.usecases.scanning.services.indexing.PythonIndexService;
import com.ibm.usecases.scanning.services.scan.ScanResultDTO;
import com.ibm.usecases.scanning.services.scan.java.JavaScannerService;
import com.ibm.usecases.scanning.services.scan.python.PythonScannerService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;

public final class ScanProcessManager extends ProcessManager<ScanId, ScanAggregate> {
    @Nonnull private final ScanId scanId;
    @Nonnull private final IProgressDispatcher progressDispatcher;
    @Nonnull private final String baseCloneDirPath;
    @Nonnull private final List<File> javaDependencyJARS;

    @Nullable private File projectDirectory;
    @Nullable private Map<Language, List<ProjectModule>> index;

    public ScanProcessManager(
            @Nonnull ScanId scanId,
            @Nonnull ICommandBus commandBus,
            @Nonnull IRepository<ScanId, ScanAggregate> repository,
            @Nonnull IProgressDispatcher progressDispatcher,
            @Nonnull IScanConfiguration iScanConfiguration) {
        super(commandBus, repository);
        this.scanId = scanId;
        this.progressDispatcher = progressDispatcher;
        this.baseCloneDirPath = iScanConfiguration.getBaseCloneDirPath();
        this.javaDependencyJARS = iScanConfiguration.getJavaDependencyJARS();
    }

    @Override
    public void handle(@Nonnull ICommand command) throws Exception {
        switch (command) {
            case FetchDataFromDepsDevCommand fetchDataFromDepsDevCommand ->
                    this.handleFetchDataFromDepsDevCommand(fetchDataFromDepsDevCommand);
            case CloneGitRepositoryCommand cloneGitRepositoryCommand ->
                    this.handleCloneGitRepositoryCommand(cloneGitRepositoryCommand);
            case IndexModulesCommand indexModulesCommand ->
                    this.handleIndexModulesCommand(indexModulesCommand);
            case ScanCommand scanCommand -> this.handleScanCommand(scanCommand);
            default -> {
                // nothing
            }
        }
    }

    private void handleFetchDataFromDepsDevCommand(@Nonnull FetchDataFromDepsDevCommand command)
            throws EntityNotFoundById,
                    NoDataAvailableInDepsDevForPurl,
                    GitUrlAlreadyResolved,
                    NoPurlSpecifiedForScan {
        if (this.scanId != command.id()) {
            return;
        }
        final Optional<ScanAggregate> possibleScanAggregate = this.repository.read(command.id());
        final ScanAggregate scanAggregate =
                possibleScanAggregate.orElseThrow(() -> new EntityNotFoundById(command.id()));
        try {
            // Fetch source repo from deps.dev
            final DepsDevService depsDev = new DepsDevService();
            final String gitUrl =
                    depsDev.fetch(
                            scanAggregate
                                    .getPurl()
                                    .map(PackageURL::canonicalize)
                                    .orElseThrow(() -> new NoPurlSpecifiedForScan(scanId)));
            // update aggregate
            scanAggregate.setResolvedGitUrl(gitUrl);
            this.repository.save(scanAggregate);

            this.commandBus.send(
                    new CloneGitRepositoryCommand(command.id(), command.credentials()));
        } catch (Exception e) {
            this.compensate(command.id());
            throw e;
        }
    }

    private void handleCloneGitRepositoryCommand(@Nonnull CloneGitRepositoryCommand command)
            throws EntityNotFoundById,
                    GitCloneFailed,
                    CommitHashAlreadyExists,
                    ClientDisconnected,
                    NoGitUrlSpecifiedForScan {
        if (this.scanId != command.id()) {
            return;
        }
        final Optional<ScanAggregate> possibleScanAggregate = this.repository.read(command.id());
        final ScanAggregate scanAggregate =
                possibleScanAggregate.orElseThrow(() -> new EntityNotFoundById(command.id()));
        final GitUrl gitUrl =
                scanAggregate
                        .getGitUrl()
                        .orElseThrow(() -> new NoGitUrlSpecifiedForScan(command.id()));
        try {
            // clone git repository
            final GitService gitService =
                    new GitService(
                            this.progressDispatcher, this.baseCloneDirPath, command.credentials());
            final CloneResultDTO cloneResultDTO;

            if (scanAggregate.getPurl().isPresent()) {
                cloneResultDTO = gitService.clone(gitUrl, scanAggregate.getPurl().get());
            } else {
                cloneResultDTO = gitService.clone(gitUrl, scanAggregate.getRevision());
            }
            this.projectDirectory = cloneResultDTO.directory();
            // update aggregate
            scanAggregate.setCommitHash(cloneResultDTO.commit());
            this.repository.save(scanAggregate);
            // start indexing
            this.commandBus.send(new IndexModulesCommand(command.id()));
        } catch (GitCloneFailed gitCloneFailed) {
            // if previous attempted failed with `main`, try `master`
            if (scanAggregate.getScanRequest().revision().value().equalsIgnoreCase("main")) {
                // delete old aggregate
                this.repository.delete(scanId);
                // emit new scan command with `master` branch
                this.commandBus.send(
                        new RequestScanCommand(
                                this.scanId,
                                gitUrl.value(),
                                "master",
                                scanAggregate.getScanRequest().subFolder(),
                                command.credentials()));
            } else {
                this.progressDispatcher.send(
                        new ProgressMessage(
                                ProgressMessageType.ERROR, gitCloneFailed.getMessage()));
                this.compensate(command.id());
                throw gitCloneFailed;
            }
        } catch (Exception e) {
            this.progressDispatcher.send(
                    new ProgressMessage(ProgressMessageType.ERROR, e.getMessage()));
            this.compensate(command.id());
            throw e;
        }
    }

    private void handleIndexModulesCommand(@Nonnull IndexModulesCommand command)
            throws EntityNotFoundById, GitCloneResultNotAvailable, ClientDisconnected {
        if (this.scanId != command.id()) {
            return;
        }

        try {
            final Optional<ScanAggregate> possibleScanAggregate =
                    this.repository.read(command.id());
            final ScanAggregate scanAggregate =
                    possibleScanAggregate.orElseThrow(() -> new EntityNotFoundById(command.id()));
            final ScanRequest scanRequest = scanAggregate.getScanRequest();
            this.index = new EnumMap<>(Language.class);
            // java
            final JavaIndexService javaIndexService = new JavaIndexService(this.progressDispatcher);
            final File dir =
                    Optional.ofNullable(this.projectDirectory)
                            .orElseThrow(GitCloneResultNotAvailable::new);
            final List<ProjectModule> javaIndex =
                    javaIndexService.index(dir, scanRequest.subFolder());
            this.index.put(Language.JAVA, javaIndex);
            // python
            final PythonIndexService pythonIndexService =
                    new PythonIndexService(this.progressDispatcher);
            final List<ProjectModule> pythonIndex =
                    pythonIndexService.index(dir, scanRequest.subFolder());
            this.index.put(Language.PYTHON, pythonIndex);
            // continue with scan
            this.commandBus.send(new ScanCommand(command.id()));
        } catch (Exception e) {
            this.progressDispatcher.send(
                    new ProgressMessage(ProgressMessageType.ERROR, e.getMessage()));
            this.compensate(command.id());
            throw e;
        }
    }

    private void handleScanCommand(@Nonnull ScanCommand command)
            throws EntityNotFoundById,
                    NoProjectDirectoryProvided,
                    NoIndexForProject,
                    NoCommitProvided,
                    ScanResultForLanguageAlreadyExists,
                    ClientDisconnected,
                    CBOMSerializationFailed,
                    NoGitUrlSpecifiedForScan {
        if (this.scanId != command.id()) {
            return;
        }

        try {
            final Optional<ScanAggregate> possibleScanAggregate =
                    this.repository.read(command.id());
            final ScanAggregate scanAggregate =
                    possibleScanAggregate.orElseThrow(() -> new EntityNotFoundById(command.id()));
            final ScanRequest scanRequest = scanAggregate.getScanRequest();
            final Commit commit = scanAggregate.getCommit().orElseThrow(NoCommitProvided::new);

            // progress scan statistics
            final long startTime = System.currentTimeMillis();
            int numberOfScannedLine;
            int numberOfScannedFiles;
            CBOM cbom = null;

            // java
            final JavaScannerService javaScannerService =
                    new JavaScannerService(
                            this.progressDispatcher,
                            this.javaDependencyJARS,
                            Optional.ofNullable(this.projectDirectory)
                                    .orElseThrow(NoProjectDirectoryProvided::new));
            final ScanResultDTO javaScanResultDTO =
                    javaScannerService.scan(
                            scanAggregate
                                    .getGitUrl()
                                    .orElseThrow(() -> new NoGitUrlSpecifiedForScan(scanId)),
                            scanAggregate.getRevision(),
                            commit,
                            scanRequest.subFolder(),
                            Optional.ofNullable(this.index)
                                    .map(i -> i.get(Language.JAVA))
                                    .orElseThrow(NoIndexForProject::new));
            // update statistics
            numberOfScannedLine = javaScanResultDTO.numberOfScannedLine();
            numberOfScannedFiles = javaScanResultDTO.numberOfScannedFiles();

            if (javaScanResultDTO.cbom() != null) {
                // update statistics
                cbom = javaScanResultDTO.cbom();

                scanAggregate.reportScanResults(
                        new LanguageScan(
                                Language.JAVA,
                                new ScanMetadata(
                                        javaScanResultDTO.startTime(),
                                        javaScanResultDTO.endTime(),
                                        javaScanResultDTO.numberOfScannedLine(),
                                        javaScanResultDTO.numberOfScannedFiles()),
                                javaScanResultDTO.cbom()));
            }

            // python
            final PythonScannerService pythonScannerService =
                    new PythonScannerService(
                            this.progressDispatcher,
                            Optional.ofNullable(this.projectDirectory)
                                    .orElseThrow(NoProjectDirectoryProvided::new));
            final ScanResultDTO pythonScanResultDTO =
                    pythonScannerService.scan(
                            scanAggregate
                                    .getGitUrl()
                                    .orElseThrow(() -> new NoGitUrlSpecifiedForScan(scanId)),
                            scanAggregate.getRevision(),
                            commit,
                            scanRequest.subFolder(),
                            Optional.ofNullable(this.index)
                                    .map(i -> i.get(Language.PYTHON))
                                    .orElseThrow(NoIndexForProject::new));
            // update statistics
            numberOfScannedLine += pythonScanResultDTO.numberOfScannedLine();
            numberOfScannedFiles += pythonScanResultDTO.numberOfScannedFiles();

            if (pythonScanResultDTO.cbom() != null) {
                // update statistics
                if (cbom != null) {
                    cbom.merge(pythonScanResultDTO.cbom());
                } else {
                    cbom = pythonScanResultDTO.cbom();
                }

                scanAggregate.reportScanResults(
                        new LanguageScan(
                                Language.PYTHON,
                                new ScanMetadata(
                                        pythonScanResultDTO.startTime(),
                                        pythonScanResultDTO.endTime(),
                                        pythonScanResultDTO.numberOfScannedLine(),
                                        pythonScanResultDTO.numberOfScannedFiles()),
                                pythonScanResultDTO.cbom()));
            }

            // publish scan finished and save state
            scanAggregate.scanFinished();
            this.repository.save(scanAggregate);

            this.progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.SCANNED_DURATION,
                            String.valueOf((System.currentTimeMillis() - startTime) / 1000)));
            this.progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.SCANNED_FILE_COUNT,
                            String.valueOf(numberOfScannedFiles)));
            this.progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.SCANNED_NUMBER_OF_LINES,
                            String.valueOf(numberOfScannedLine)));
            this.progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.CBOM,
                            Optional.ofNullable(cbom)
                                    .orElseThrow(CBOMSerializationFailed::new)
                                    .toJSON()
                                    .toString()));
            this.progressDispatcher.send(
                    new ProgressMessage(ProgressMessageType.LABEL, "Finished"));
        } catch (Exception e) {
            this.progressDispatcher.send(
                    new ProgressMessage(ProgressMessageType.ERROR, e.getMessage()));
            this.compensate(command.id());
            throw e;
        } finally {
            this.compensate(command.id());
        }
    }

    @Override
    public void compensate(@Nonnull ScanId id) {
        // unregister process manager
        this.commandBus.remove(this);
        // remove cloned repo
        Optional.ofNullable(this.projectDirectory)
                .ifPresent(
                        dir -> {
                            try {
                                FileUtils.deleteDirectory(dir);
                            } catch (Exception ignored) {
                                // ignore
                            }
                        });
    }
}
