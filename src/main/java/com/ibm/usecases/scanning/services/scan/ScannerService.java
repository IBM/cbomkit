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
package com.ibm.usecases.scanning.services.scan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.Revision;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import com.ibm.mapper.model.INode;
import com.ibm.output.IOutputFileFactory;
import com.ibm.output.cyclondx.CBOMOutputFile;
import com.ibm.output.cyclondx.CBOMOutputFileFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScannerService implements IScannerService {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ScannerService.class);

    @Nonnull protected final IProgressDispatcher progressDispatcher;
    @Nonnull protected final File projectDirectory;
    @Nonnull protected final CBOMOutputFile cbomOutputFile;

    protected ScannerService(
            @Nonnull IProgressDispatcher progressDispatcher, @Nonnull File projectDirectory) {
        this.progressDispatcher = progressDispatcher;
        this.projectDirectory = projectDirectory;
        this.cbomOutputFile = new CBOMOutputFile();
    }

    @Override
    public void accept(@Nonnull final List<INode> nodes) {
        synchronized (this) {
            this.cbomOutputFile.add(nodes);
        }
        // emit
        final CBOMOutputFileFactory fileFactory = new CBOMOutputFileFactory();
        final CBOMOutputFile componentAsCBOM = fileFactory.createOutputFormat(nodes);
        componentAsCBOM
                .getBom()
                .getComponents()
                .forEach(
                        component -> {
                            sanitizeOccurrence(this.projectDirectory, component);
                            try {
                                this.progressDispatcher.send(
                                        new ProgressMessage(
                                                ProgressMessageType.DETECTION,
                                                new ObjectMapper().writeValueAsString(component)));
                            } catch (JsonProcessingException | ClientDisconnected e) {
                                LOGGER.error(e.getMessage());
                            }
                        });
    }

    @Nonnull
    protected synchronized Optional<Bom> receiveBom(
            @Nonnull File projectDirectory,
            @Nonnull GitUrl gitUrl,
            @Nonnull Revision revision,
            @Nonnull Commit commit,
            @Nullable String subFolder) {

        final Bom bom = this.cbomOutputFile.getBom();
        // sanitizeOccurrence
        bom.getComponents().forEach(component -> sanitizeOccurrence(projectDirectory, component));
        // add metadata
        final Metadata metadata = new Metadata();

        final Property gitUrlProperty = new Property();
        gitUrlProperty.setName("gitUrl");
        gitUrlProperty.setValue(gitUrl.value());
        metadata.addProperty(gitUrlProperty);

        final Property revisionProperty = new Property();
        revisionProperty.setName("revision");
        revisionProperty.setValue(revision.value());
        metadata.addProperty(revisionProperty);

        final Property commitProperty = new Property();
        commitProperty.setName("commit");
        commitProperty.setValue(commit.hash());
        metadata.addProperty(commitProperty);

        if (subFolder != null) {
            final Property subFolderProperty = new Property();
            subFolderProperty.setName("subfolder");
            subFolderProperty.setValue(subFolder);
            metadata.addProperty(subFolderProperty);
        }

        bom.setMetadata(metadata);
        // reset scanner
        final com.ibm.plugin.ScannerManager scannerMgr =
                new com.ibm.plugin.ScannerManager(IOutputFileFactory.DEFAULT);
        scannerMgr.reset();

        return Optional.of(bom);
    }

    static void sanitizeOccurrence(
            @Nonnull final File projectDirectory, @Nonnull Component component) {
        List<Occurrence> occurrenceList =
                Optional.ofNullable(component.getEvidence())
                        .map(Evidence::getOccurrences)
                        .orElse(Collections.emptyList());

        if (occurrenceList.isEmpty()) {
            return;
        }
        final String baseDirPath = projectDirectory.getAbsolutePath();
        occurrenceList.forEach(
                occurrence -> {
                    if (occurrence.getLocation().startsWith(baseDirPath)) {
                        occurrence.setLocation(
                                occurrence.getLocation().substring(baseDirPath.length() + 1));
                    }
                });
    }
}
