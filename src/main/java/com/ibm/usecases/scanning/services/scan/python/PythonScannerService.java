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
package com.ibm.usecases.scanning.services.scan.python;

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
import org.sonar.plugins.python.api.PythonCheck;
import org.sonar.plugins.python.api.PythonVisitorContext;
import org.sonar.plugins.python.api.tree.FileInput;

public final class PythonScannerService extends ScannerService {

    public PythonScannerService(
            @Nonnull IProgressDispatcher progressDispatcher, @Nonnull File projectDirectory) {
        super(progressDispatcher, projectDirectory);
    }

    @Override
    public @Nonnull ScanResultDTO scan(
            @Nonnull GitUrl gitUrl,
            @Nonnull Revision revision,
            @Nonnull Commit commit,
            @Nullable String subFolder,
            @Nonnull List<ProjectModule> index)
            throws ClientDisconnected {
        final PythonCheck visitor = new PythonDetectionCollectionRule(this);

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

            for (InputFile inputFile : project.inputFileList()) {
                final PythonScannableFile pythonScannableFile = new PythonScannableFile(inputFile);
                final FileInput parsedFile = pythonScannableFile.parse();
                final PythonVisitorContext context =
                        new PythonVisitorContext(
                                parsedFile,
                                pythonScannableFile,
                                this.projectDirectory,
                                project.identifier());
                visitor.scanFile(context);
            }
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
