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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.infrastructure.compliance.ComplianceResult;
import com.ibm.infrastructure.compliance.ComplianceResultFactory;
import com.ibm.infrastructure.compliance.EvaluationStatus;
import com.ibm.infrastructure.compliance.service.ComplianceCheckResultDTO;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OPAComplianceServiceTest {
    @Test
    void repeatedRuleFindingsCannotInflateCoverage() throws Exception {
        String json =
                """
                {"result":{"findings":[
                  {"rule":"asymmetric_quantum_safe","result":"quantum-safe","bom-ref":"mlkem"},
                  {"rule":"nist_qs_level","result":"quantum-safe","bom-ref":"mlkem"}
                ]}}
                """;
        OPAResponse response = new ObjectMapper().readValue(json, OPAResponse.class);
        ComplianceCheckResultDTO check =
                new ComplianceCheckResultDTO(OPAComplianceService.getFindings(response), false);
        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(
                        "opa",
                        "quantum_safe",
                        check,
                        OPAResult.getComplianceLevels(),
                        OPAResult.getComplianceLevel(OPAResult.UNKNOWN).id(),
                        Set.of("mlkem", "uncovered"));
        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.PARTIAL);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().evaluatedAssets()).isEqualTo(1);
    }
}
