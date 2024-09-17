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
package com.ibm.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.jboss.logging.Logger;

public class GitService {

    private static final Logger LOG = Logger.getLogger(GitService.class);

    @Nonnull private final String gitUrl;
    private final String revision;

    public GitService(@Nonnull String gitUrl, @Nonnull String revision) {
        this.gitUrl = gitUrl;
        this.revision = revision;
    }

    @Nonnull
    public String cloneRepository(
            @Nonnull File clonePath, @Nonnull Consumer<String> messageHandling)
            throws GitAPIException, IOException {
        GitProgressMonitor progressMonitor = new GitProgressMonitor(messageHandling);
        Git clonedRepo =
                Git.cloneRepository()
                        .setProgressMonitor(progressMonitor)
                        .setURI(gitUrl)
                        .setBranch(revision)
                        .setDirectory(clonePath)
                        .call();

        Ref revisionRef = clonedRepo.getRepository().findRef(revision);
        if (revisionRef == null) {
            throw new NullPointerException();
        }

        ObjectId commit = revisionRef.getObjectId();
        if (commit == null) {
            throw new NullPointerException();
        }

        return Objects.requireNonNull(commit.getName());
    }

    @Nonnull
    public File createCloneDirectory(
            @Nonnull String path, @Nonnull Supplier<String> dirNameGenerationFunction)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash =
                digest.digest(dirNameGenerationFunction.get().getBytes(StandardCharsets.UTF_8));
        String base64Hash = Base64.getEncoder().encodeToString(encodedHash).replace('/', '_');
        String clonePath = path + File.separator + base64Hash;

        final File clonedRepo = new File(clonePath);
        if (clonedRepo.exists()) {
            LOG.info("Clone dir already exists: " + clonedRepo.getPath());
        }
        clonedRepo.mkdirs();
        return clonedRepo;
    }
}
