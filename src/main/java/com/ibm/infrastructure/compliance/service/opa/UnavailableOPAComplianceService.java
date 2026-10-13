/*
 * CBOMkit
 * Copyright (C) 2026 PQCA
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
package com.ibm.infrastructure.compliance.service.opa;

import com.ibm.domain.compliance.PolicyIdentifier;
import com.ibm.infrastructure.compliance.ComplianceLevel;
import com.ibm.infrastructure.compliance.service.ComplianceCheckResultDTO;
import com.ibm.infrastructure.compliance.service.IComplianceService;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.pqca.scanning.CBOM;

/** Keeps a configured-but-unavailable OPA policy from silently becoming a local green verdict. */
public final class UnavailableOPAComplianceService implements IComplianceService {
    @Override
    public @Nonnull String getName() {
        return "Open Policy Agent Compliance Service (unavailable)";
    }

    @Override
    public @Nonnull List<ComplianceLevel> getComplianceLevels() {
        return OPAResult.getComplianceLevels();
    }

    @Override
    public @Nonnull ComplianceLevel getDefaultComplianceLevel() {
        return OPAResult.getComplianceLevel(OPAResult.UNKNOWN);
    }

    @Override
    public @Nonnull ComplianceCheckResultDTO evaluate(
            @Nonnull PolicyIdentifier policyIdentifier, @Nonnull CBOM cbom) {
        return new ComplianceCheckResultDTO(null, true);
    }
}
