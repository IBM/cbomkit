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
package com.ibm.usecases.compliance.service;

import com.ibm.domain.compliance.CryptographicAsset;
import com.ibm.domain.scanning.CBOM;
import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.errors.CBOMSerializationFailed;
import com.ibm.domain.scanning.errors.InvalidScanUrl;
import com.ibm.infrastructure.database.readmodels.CBOMReadModel;
import com.ibm.infrastructure.database.readmodels.ICBOMReadRepository;
import com.ibm.usecases.compliance.errors.CouldNotFindCBOMForGitRepository;
import com.ibm.usecases.compliance.errors.ErrorWhileParsingStringToCBOM;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.parsers.Parser;

public final class CompliancePreparationService {

    public Collection<CryptographicAsset> receiveCryptographicAssets(
            @Nonnull ICBOMReadRepository readRepository,
            @Nonnull GitUrl gitUrl,
            @Nullable Commit commit)
            throws CBOMSerializationFailed, CouldNotFindCBOMForGitRepository, InvalidScanUrl {
        // validate
        gitUrl.validate();

        final CBOMReadModel cbomReadModel;
        if (commit != null) {
            cbomReadModel =
                    readRepository
                            .findBy(gitUrl, commit)
                            .orElseThrow(
                                    () -> new CouldNotFindCBOMForGitRepository(gitUrl.value()));
        } else {
            cbomReadModel =
                    readRepository
                            .findBy(gitUrl)
                            .orElseThrow(
                                    () -> new CouldNotFindCBOMForGitRepository(gitUrl.value()));
        }
        final CBOM cbom = CBOM.formJSON(cbomReadModel.getBom());
        return cbom.cycloneDXbom().getComponents().stream()
                .map(component -> new CryptographicAsset(component.getBomRef(), component))
                .toList();
    }

    public Collection<CryptographicAsset> transformCBOMString(@Nonnull String cbomString)
            throws ErrorWhileParsingStringToCBOM {
        try {
            byte[] cbomBytes = cbomString.getBytes(StandardCharsets.UTF_8);
            // Create the parser
            Parser parser = BomParserFactory.createParser(cbomBytes);
            // Parse the BOM content
            Bom cycloneDXbom = parser.parse(cbomBytes);
            return cycloneDXbom.getComponents().stream()
                    .map(component -> new CryptographicAsset(component.getBomRef(), component))
                    .toList();
        } catch (ParseException e) {
            throw new ErrorWhileParsingStringToCBOM(e);
        }
    }
}
