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
import com.ibm.infrastructure.compliance.service.ICryptographicAssetPolicyResult;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.pqca.errors.CBOMSerializationFailed;
import org.pqca.scanning.CBOM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OPAComplianceService implements IComplianceService {
    // private static final String PACKAGE = "policies";
    private static final String FINDINGS = "findings";

    private static final Logger LOGGER = LoggerFactory.getLogger(OPAComplianceService.class);

    @Inject @RestClient OPAService opaService;

    @Override
    public String getName() {
        return "Open Policy Agent Compliance Service";
    }

    @Override
    public List<ComplianceLevel> getComplianceLevels() {
        return OPAResult.getComplianceLevels();
    }

    @Override
    public ComplianceLevel getDefaultComplianceLevel() {
        return OPAResult.getComplianceLevel(OPAResult.UNKNOWN);
    }

    @Override
    public ComplianceCheckResultDTO evaluate(PolicyIdentifier policyIdentifier, CBOM cbom) {
        LOGGER.info("Checking CBOM with compliance policy {}", policyIdentifier.id());

        try {
            OPAResponse opaResponse = opaService.evaluate(policyIdentifier.id(), wrapInput(cbom));
            // Empty / missing findings must not be treated as quantum-safe by callers.
            // ComplianceResultFactory maps an empty finding set to NOT_EVALUATED.
            return new ComplianceCheckResultDTO(getFindings(opaResponse), false);
        } catch (Exception e) {
            LOGGER.error("OPA compliance evaluation failed: {}", e.getMessage());
        }

        return new ComplianceCheckResultDTO(null, true);
    }

    private String wrapInput(CBOM cbom) throws CBOMSerializationFailed {
        return "{\"input\":" + cbom.toJSON().toString() + "}";
    }

    static List<ICryptographicAssetPolicyResult> getFindings(OPAResponse opaResponse) {
        // Distinguish "no findings produced" (empty list → NOT_EVALUATED upstream) from
        // transport/parse failures (null results + error=true). Do not invent a green verdict.
        if (opaResponse == null
                || opaResponse.noFindings()
                || !opaResponse.getResult().containsKey(FINDINGS)) {
            LOGGER.warn(
                    "OPA returned no findings for the requested policy (missing policy, empty"
                            + " result, or response without a '{}' key)",
                    FINDINGS);
            return Collections.emptyList();
        }

        return opaResponse.getResult().get(FINDINGS).stream()
                .map(ICryptographicAssetPolicyResult.class::cast)
                .toList();
    }

    public OPAComplianceService() throws Exception {
        Optional<String> opaApiBase =
                ConfigProvider.getConfig()
                        .getOptionalValue("cbomkit.ext-policies.opa-api-base", String.class);
        if (!opaApiBase.isPresent()) {
            throw new InvalidParameterException("No ext. compliance service confifured");
        }
        opaService =
                QuarkusRestClientBuilder.newBuilder()
                        .baseUri(URI.create(opaApiBase.get()))
                        .build(OPAService.class);
        opaService.checkHealth();
        LOGGER.info("Using external compliance service: Open Policy Agent {}", opaApiBase.get());
    }
}
