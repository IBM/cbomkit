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
package com.ibm.domain.scanning;

import app.bootstrap.core.ddd.AggregateRoot;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.errors.CommitHashAlreadyExists;
import com.ibm.domain.scanning.errors.InvalidGitUrl;
import com.ibm.domain.scanning.errors.ScanResultForLanguageAlreadyExists;
import com.ibm.domain.scanning.events.CommitHashIdentifiedEvent;
import com.ibm.domain.scanning.events.LanguageScanDoneEvent;
import com.ibm.domain.scanning.events.ScanFinishedEvent;
import com.ibm.domain.scanning.events.ScanRequestedEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScanAggregate extends AggregateRoot<ScanId> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanAggregate.class);

    @Nonnull private final ScanRequest scanRequest;
    @Nullable private Commit commit;
    @Nullable private Map<Language, LanguageScan> languageScans;

    private ScanAggregate(@Nonnull final ScanId id, @Nonnull final ScanRequest scanRequest) {
        super(id, new ArrayList<>());
        this.scanRequest = scanRequest;
    }

    private ScanAggregate(
            @Nonnull ScanId id,
            @Nonnull ScanRequest scanRequest,
            @Nullable Commit commit,
            @Nullable Map<Language, LanguageScan> languageScans) {
        super(id, new ArrayList<>());
        this.scanRequest = scanRequest;
        this.commit = commit;
        this.languageScans = languageScans;
    }

    @Nonnull
    public static ScanAggregate requestScan(
            @Nonnull ScanId scanId,
            @Nonnull final ScanRequest scanRequest,
            @Nullable ICredentials credentials)
            throws InvalidGitUrl {
        // validate value object
        scanRequest.validate();
        // create aggregate
        final ScanAggregate aggregate =
                new ScanAggregate(scanId, scanRequest); // change state: start a scan
        // add domain event, uncommited!
        aggregate.apply(new ScanRequestedEvent(aggregate.getId(), credentials));
        return aggregate;
    }

    public void setCommitHash(@Nonnull Commit commit) throws CommitHashAlreadyExists {
        if (this.commit != null) {
            throw new CommitHashAlreadyExists(this.getId());
        }
        this.commit = commit;
        this.apply(new CommitHashIdentifiedEvent(this.getId()));
    }

    public void reportScanResults(@Nonnull LanguageScan scan)
            throws ScanResultForLanguageAlreadyExists {
        if (languageScans == null) {
            languageScans = new EnumMap<>(Language.class);
        }

        if (languageScans.get(scan.language()) != null) {
            throw new ScanResultForLanguageAlreadyExists(this.getId(), scan.language());
        }
        this.languageScans.put(scan.language(), scan);
        this.apply(new LanguageScanDoneEvent(this.getId(), scan.language()));
    }

    public void scanFinished() {
        this.apply(new ScanFinishedEvent(this.getId()));
    }

    @Nonnull
    public ScanRequest getScanRequest() {
        return scanRequest;
    }

    @Nonnull
    public Optional<Commit> getCommit() {
        return Optional.ofNullable(commit);
    }

    @Nonnull
    public Optional<List<LanguageScan>> getLanguageScans() {
        return Optional.ofNullable(languageScans).map(Map::values).map(ArrayList::new);
    }

    @Nonnull
    public Optional<LanguageScan> getLanguageScan(@Nonnull Language language) {
        if (languageScans == null) {
            languageScans = new EnumMap<>(Language.class);
        }
        return Optional.ofNullable(this.languageScans.get(language));
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * This function should only be used by the repository to restore the aggregate from the data
     * source.
     */
    @Nonnull
    public static ScanAggregate reconstruct(
            @Nonnull ScanId id,
            @Nonnull ScanRequest scanRequest,
            @Nullable Commit commit,
            @Nullable Map<Language, LanguageScan> languageScans) {
        return new ScanAggregate(id, scanRequest, commit, languageScans);
    }
}
