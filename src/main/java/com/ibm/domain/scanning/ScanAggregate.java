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
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import com.ibm.domain.scanning.authentication.ICredentials;
import com.ibm.domain.scanning.errors.CommitHashAlreadyExists;
import com.ibm.domain.scanning.errors.GitUrlAlreadyResolved;
import com.ibm.domain.scanning.errors.InvalidScanUrl;
import com.ibm.domain.scanning.errors.NoValidProjectIdentifierForScan;
import com.ibm.domain.scanning.errors.PackageFolderAlreadyExists;
import com.ibm.domain.scanning.errors.RevisionAlreadyExists;
import com.ibm.domain.scanning.errors.ScanResultForLanguageAlreadyExists;
import com.ibm.domain.scanning.events.CommitHashIdentifiedEvent;
import com.ibm.domain.scanning.events.GitUrlResolvedEvent;
import com.ibm.domain.scanning.events.LanguageScanDoneEvent;
import com.ibm.domain.scanning.events.PackageFolderResolvedEvent;
import com.ibm.domain.scanning.events.PurlScanRequestedEvent;
import com.ibm.domain.scanning.events.RevisionIdentifiedEvent;
import com.ibm.domain.scanning.events.ScanFinishedEvent;
import com.ibm.domain.scanning.events.ScanRequestedEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ScanAggregate extends AggregateRoot<ScanId> {
    @Nullable private GitUrl gitUrl;
    @Nullable private PackageURL purl;
    @Nonnull private Revision revision;
    @Nullable private Path packageFolder;
    @Nullable private Commit commit;
    @Nullable private Map<Language, LanguageScan> languageScans;

    public static final Revision REVISION_MAIN = new Revision("main");

    private ScanAggregate(@Nonnull final ScanId id, @Nonnull final ScanRequest scanRequest) {
        super(id, new ArrayList<>());
        try {
            this.purl = new PackageURL(scanRequest.scanUrl().value());
            if (purl.getType().equals(PackageURL.StandardTypes.GITHUB)) {
                if (purl.getQualifiers() != null && purl.getQualifiers().containsKey("branch")) {
                    this.revision = new Revision(purl.getQualifiers().get("branch"));
                } else {
                    this.revision = REVISION_MAIN;
                }
            } else {
                this.revision = new Revision(purl.getVersion());
            }
        } catch (MalformedPackageURLException e) {
            this.gitUrl = new GitUrl(scanRequest.scanUrl().value());
            this.revision = scanRequest.revision();
            this.packageFolder =
                    Optional.ofNullable(scanRequest.subFolder()).map(Path::of).orElse(null);
        }
    }

    private ScanAggregate(
            @Nonnull ScanId id,
            @Nullable GitUrl gitUrl,
            @Nullable PackageURL purl,
            @Nonnull Revision revision,
            @Nullable Path packageFolder,
            @Nullable Commit commit,
            @Nullable Map<Language, LanguageScan> languageScans) {
        super(id, new ArrayList<>());
        this.gitUrl = gitUrl;
        this.purl = purl;
        this.revision = revision;
        this.packageFolder = packageFolder;
        this.commit = commit;
        this.languageScans = languageScans;
    }

    @Nonnull
    public static ScanAggregate requestScan(
            @Nonnull ScanId scanId,
            @Nonnull final ScanRequest scanRequest,
            @Nullable ICredentials credentials)
            throws InvalidScanUrl {
        // validate value object
        scanRequest.validate();
        // create aggregate
        final ScanAggregate aggregate =
                new ScanAggregate(scanId, scanRequest); // change state: start a scan
        // add domain event, uncommited!
        if (aggregate.getPurl().isPresent()) {
            aggregate.apply(new PurlScanRequestedEvent(aggregate.getId(), credentials));
        } else {
            aggregate.apply(new ScanRequestedEvent(aggregate.getId(), credentials));
        }
        return aggregate;
    }

    public void setResolvedGitUrl(@Nonnull GitUrl gitUrl) throws GitUrlAlreadyResolved {
        if (this.gitUrl != null) {
            throw new GitUrlAlreadyResolved(this.getId());
        }
        this.gitUrl = gitUrl;
        this.apply(new GitUrlResolvedEvent(this.getId()));
    }

    public void setRevision(@Nonnull String revision) throws RevisionAlreadyExists {
        if (this.revision != null) {
            throw new RevisionAlreadyExists(this.getId());
        }
        this.revision = new Revision(revision);
        this.apply(new RevisionIdentifiedEvent(this.getId()));
    }

    public void setCommitHash(@Nonnull Commit commit) throws CommitHashAlreadyExists {
        if (this.commit != null) {
            throw new CommitHashAlreadyExists(this.getId());
        }
        this.commit = commit;
        this.apply(new CommitHashIdentifiedEvent(this.getId()));
    }

    public void setPackageFolder(@Nonnull Path packageFolder) throws PackageFolderAlreadyExists {
        if (this.packageFolder != null) {
            throw new PackageFolderAlreadyExists(this.getId());
        }
        this.packageFolder = packageFolder;
        this.apply(new PackageFolderResolvedEvent(this.getId()));
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
    public Optional<Commit> getCommit() {
        return Optional.ofNullable(commit);
    }

    @Nonnull
    public Optional<PackageURL> getPurl() {
        return Optional.ofNullable(purl);
    }

    @Nonnull
    public Optional<GitUrl> getGitUrl() {
        return Optional.ofNullable(gitUrl);
    }

    @Nonnull
    public Revision getRevision() {
        return revision;
    }

    @Nonnull
    public Optional<Path> getPackageFolder() {
        return Optional.ofNullable(packageFolder);
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

    @Nonnull
    public String getProjectIdentifier() throws NoValidProjectIdentifierForScan {
        if (this.purl == null && this.gitUrl == null) {
            throw new NoValidProjectIdentifierForScan(this.getId());
        }

        if (this.purl != null) {
            return this.purl.toString();
        }

        try {
            final URI uri = new URI(this.gitUrl.value());
            final String[] pathSegments = uri.getPath().split("/");

            if (pathSegments.length < 3) {
                throw new IllegalArgumentException("Invalid Git URL format");
            }

            final String namespace =
                    "github.com".equals(uri.getHost())
                            ? pathSegments[1]
                            : uri.getHost() + "/" + pathSegments[1];
            final String name = pathSegments[2].replaceAll("\\.git$", "");

            final PackageURLBuilder purlBuilder =
                    PackageURLBuilder.aPackageURL()
                            .withType("github")
                            .withNamespace(namespace)
                            .withName(name)
                            .withVersion(
                                    Optional.ofNullable(this.commit).map(Commit::hash).orElse(null))
                            .withSubpath(
                                    Optional.ofNullable(this.packageFolder)
                                            .map(Path::toString)
                                            .orElse(null));
            if (!this.revision.value().equals("main")) {
                purlBuilder.withQualifier("branch", this.revision.value());
            }
            return purlBuilder.build().toString();
        } catch (Exception e) {
            throw new NoValidProjectIdentifierForScan(this.getId());
        }
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
            @Nullable GitUrl gitUrl,
            @Nullable PackageURL purl,
            @Nonnull Revision revision,
            @Nullable Path packageFolder,
            @Nullable Commit commit,
            @Nullable Map<Language, LanguageScan> languageScans) {
        return new ScanAggregate(id, gitUrl, purl, revision, packageFolder, commit, languageScans);
    }
}
