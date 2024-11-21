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
package com.ibm.usecases.scanning.services.git;

import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.ScanRequest;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.authentication.PersonalAccessToken;
import com.ibm.domain.scanning.authentication.UsernameAndPasswordCredentials;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import com.ibm.usecases.scanning.errors.GitCloneFailed;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public final class GitService {
    @Nonnull private final IProgressDispatcher progressDispatcher;
    @Nonnull private final String baseCloneDirPath;

    public GitService(
            @Nonnull IProgressDispatcher progressDispatcher, @Nonnull String baseCloneDirPath) {
        this.progressDispatcher = progressDispatcher;
        this.baseCloneDirPath = baseCloneDirPath;
    }

    @Nonnull
    public CloneResultDTO clone(
            @Nonnull ScanRequest scanRequest, @Nullable ICredentials credentials)
            throws GitCloneFailed, ClientDisconnected {
        try {
            // create directory
            final String folderId = UUID.randomUUID().toString().replace("-", "");
            final String scanClonePath = this.baseCloneDirPath + File.separator + folderId;
            final File scanCloneFile = new File(scanClonePath);
            if (scanCloneFile.exists()) {
                throw new GitCloneFailed("Clone dir already exists " + scanCloneFile.getPath());
            }
            if (!scanCloneFile.mkdirs()) {
                throw new GitCloneFailed("Could not create " + scanCloneFile.getPath());
            }
            // clone
            final GitProgressMonitor gitProgressMonitor =
                    new GitProgressMonitor(
                            progressMessage -> {
                                try {
                                    this.progressDispatcher.send(
                                            new ProgressMessage(
                                                    ProgressMessageType.LABEL, progressMessage));
                                } catch (ClientDisconnected e) {
                                    // nothing
                                }
                            });

            final Git clonedRepo =
                    Git.cloneRepository()
                            .setProgressMonitor(gitProgressMonitor)
                            .setURI(scanRequest.gitUrl().value())
                            .setBranch(scanRequest.revision().value())
                            .setDirectory(scanCloneFile)
                            .setCredentialsProvider(getCredentialsProvider(credentials))
                            .call();
            Ref revisionRef = clonedRepo.getRepository().findRef(scanRequest.revision().value());
            if (revisionRef == null) {
                throw new GitCloneFailed("Revision not found: " + scanRequest.revision().value());
            }

            ObjectId commitHash = revisionRef.getObjectId();
            if (commitHash == null) {
                throw new GitCloneFailed(
                        "Commit not found for revision: " + scanRequest.revision().value());
            }

            final Commit commit = new Commit(commitHash.getName());
            this.progressDispatcher.send(
                    new ProgressMessage(
                            ProgressMessageType.BRANCH, scanRequest.revision().value()));
            this.progressDispatcher.send(
                    new ProgressMessage(ProgressMessageType.REVISION_HASH, commit.hash()));
            return new CloneResultDTO(commit, scanCloneFile);
        } catch (GitAPIException | GitCloneFailed | IOException e) {
            throw new GitCloneFailed("Git clone failed: " + e.getMessage());
        }
    }

    @Nullable private CredentialsProvider getCredentialsProvider(@Nullable ICredentials credentials) {
        if (credentials
                instanceof
                UsernameAndPasswordCredentials(
                        @Nonnull String username,
                        @Nonnull String password)) {
            return new UsernamePasswordCredentialsProvider(username, password);
        } else if (credentials instanceof PersonalAccessToken(@Nonnull String token)) {
            return new UsernamePasswordCredentialsProvider(token, "");
        }
        return null;
    }
}
