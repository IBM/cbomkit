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
package com.ibm.compliance.ibmregulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ibm.compliance.ComplianceFormat;
import com.ibm.compliance.IComplianceService;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public class IBMRegulatorService implements IComplianceService {

    @Nonnull private final IBMRegulatorClient client;
    @Nonnull private static final String COMPLIANCE_SERVICE_NAME = "IBM Regulator";

    public IBMRegulatorService(@NotNull IBMRegulatorClient client) {
        this.client = client;
    }

    @Override
    public @NotNull ComplianceFormat check(
            @NotNull String policyIdentifier, @NotNull String cbomString) {
        String res = client.check(policyIdentifier, cbomString);
        return parse(res);
    }

    // Better use an ObjectMapper based on the regulator model classes (if open-sourced)
    private @NotNull ComplianceFormat parse(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(jsonString);

            String policyName = rootNode.path("policyDocumentTitle").asText();
            boolean compliant = rootNode.path("compliant").asBoolean();

            List<ComplianceFormat.ComplianceFinding> findings = new ArrayList<>();
            ArrayNode findingsArray = (ArrayNode) rootNode.path("findings");
            for (JsonNode findingNode : findingsArray) {
                String bomRef = findingNode.path("bom-ref").asText();
                String errorMessage = findingNode.path("errorMessage").asText();
                ComplianceFormat.ComplianceFinding finding =
                        new ComplianceFormat.ComplianceFinding(bomRef, 1, errorMessage);
                findings.add(finding);
            }

            List<ComplianceFormat.ComplianceLevel> complianceLevels = new ArrayList<>();
            complianceLevels.add(
                    new ComplianceFormat.ComplianceLevel(
                            1,
                            "Not Quantum Safe",
                            "#fac532",
                            ComplianceFormat.ComplianceIcon.WARNING));
            complianceLevels.add(
                    new ComplianceFormat.ComplianceLevel(
                            2,
                            "Quantum Safe",
                            "green",
                            ComplianceFormat.ComplianceIcon.CHECKMARK_SECURE));

            return new ComplianceFormat(
                    COMPLIANCE_SERVICE_NAME, policyName, findings, complianceLevels, 2, compliant);

        } catch (JsonProcessingException e) {
            return ComplianceFormat.error(COMPLIANCE_SERVICE_NAME);
        }
    }
}
