/*
 * CBOMkit
 * Copyright (C) 2024 PQCA
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
package com.ibm.usecases.compliance.queries;

import app.bootstrap.core.cqrs.IQueryBus;
import app.bootstrap.core.cqrs.QueryHandler;
import com.ibm.domain.compliance.PolicyIdentifier;
import com.ibm.infrastructure.compliance.ComplianceResult;
import com.ibm.infrastructure.compliance.ComplianceResultFactory;
import com.ibm.infrastructure.compliance.IComplianceConfiguration;
import com.ibm.infrastructure.compliance.service.ComplianceCheckResultDTO;
import com.ibm.infrastructure.compliance.service.IComplianceService;
import com.ibm.infrastructure.database.readmodels.ICBOMReadRepository;
import com.ibm.usecases.compliance.service.CompliancePreparationService;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.cyclonedx.model.Component;
import org.pqca.scanning.CBOM;

@Singleton
public final class RequestComplianceCheckForScannedGitRepositoryQueryHandler
        extends QueryHandler<RequestComplianceCheckForScannedGitRepositoryQuery, ComplianceResult> {
    @Nonnull private final ICBOMReadRepository readRepository;
    @Nonnull private final IComplianceService complianceService;

    void onStart(@Observes StartupEvent event) {
        this.queryBus.register(this, RequestComplianceCheckForScannedGitRepositoryQuery.class);
    }

    public RequestComplianceCheckForScannedGitRepositoryQueryHandler(
            @Nonnull IQueryBus queryBus,
            @Nonnull ICBOMReadRepository readRepository,
            @Nonnull IComplianceConfiguration complianceConfiguration) {
        super(queryBus);
        this.readRepository = readRepository;
        this.complianceService = complianceConfiguration.getComplianceService();
    }

    @Override
    public @Nonnull ComplianceResult handle(
            @Nonnull
                    RequestComplianceCheckForScannedGitRepositoryQuery
                            requestComplianceCheckForScannedGitRepositoryQuery)
            throws Exception {
        final CompliancePreparationService compliancePreparationService =
                new CompliancePreparationService();
        final CBOM cbom =
                compliancePreparationService.receiveCryptographicAssets(
                        this.readRepository,
                        requestComplianceCheckForScannedGitRepositoryQuery.projectIdentifier());

        final PolicyIdentifier policyIdentifier =
                new PolicyIdentifier(
                        requestComplianceCheckForScannedGitRepositoryQuery.policyIdentifier());
        final ComplianceCheckResultDTO complianceCheckResultDTO =
                this.complianceService.evaluate(policyIdentifier, cbom);

        return ComplianceResultFactory.fromServiceResult(
                this.complianceService.getName(),
                policyIdentifier.id(),
                complianceCheckResultDTO,
                this.complianceService.getComplianceLevels(),
                this.complianceService.getDefaultComplianceLevel().id(),
                cryptographicAssetIds(cbom));
    }

    private static Set<String> cryptographicAssetIds(@Nonnull CBOM cbom) {
        Set<String> assetIds = new HashSet<>();
        AtomicInteger invalidBomRef = new AtomicInteger();
        Optional.ofNullable(cbom.cycloneDXbom().getComponents()).orElseGet(List::of).stream()
                .filter(component -> component.getType() == Component.Type.CRYPTOGRAPHIC_ASSET)
                .forEach(
                        component -> {
                            String id = component.getBomRef();
                            if (id == null || id.isBlank() || !assetIds.add(id)) {
                                // Missing or duplicate identities must not reduce the denominator.
                                String syntheticId;
                                do {
                                    syntheticId =
                                            "__invalid_input_bom_ref_"
                                                    + invalidBomRef.getAndIncrement();
                                } while (!assetIds.add(syntheticId));
                            }
                        });
        return Set.copyOf(assetIds);
    }
}
