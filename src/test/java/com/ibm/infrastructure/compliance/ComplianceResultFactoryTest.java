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
package com.ibm.infrastructure.compliance;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.infrastructure.compliance.service.BasicCryptographicAssetPolicyResult;
import com.ibm.infrastructure.compliance.service.ComplianceCheckResultDTO;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComplianceResultFactoryTest {

    private static final String SERVICE = "test-service";
    private static final String POLICY = "quantum_safe";

    private static final ComplianceLevel VULNERABLE =
            new ComplianceLevel(
                    1,
                    "Not Quantum Safe",
                    null,
                    "#fac532",
                    ComplianceLevel.ComplianceIcon.WARNING,
                    true);
    private static final ComplianceLevel UNKNOWN =
            new ComplianceLevel(
                    2,
                    "Unknown",
                    "Unknown Compliance",
                    "#17a9d1",
                    ComplianceLevel.ComplianceIcon.UNKNOWN,
                    true);
    private static final ComplianceLevel SAFE =
            new ComplianceLevel(
                    3,
                    "Quantum Safe",
                    null,
                    "green",
                    ComplianceLevel.ComplianceIcon.CHECKMARK_SECURE,
                    false);
    private static final ComplianceLevel NA =
            new ComplianceLevel(
                    4,
                    "Not Applicable",
                    "Not Applicable",
                    "gray",
                    ComplianceLevel.ComplianceIcon.NOT_APPLICABLE,
                    false);

    private static final List<ComplianceLevel> LEVELS = List.of(VULNERABLE, UNKNOWN, SAFE, NA);

    @Test
    @DisplayName("Empty findings for a non-empty CBOM are not_evaluated, never quantum-safe")
    void emptyFindingsAreNotEvaluated() {
        ComplianceResult result =
                ComplianceResultFactory.fromFindings(SERVICE, POLICY, List.of(), LEVELS, 2, 3);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.NOT_EVALUATED);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.error()).isFalse();
        assertThat(result.evaluationSummary().totalCryptographicAssets()).isEqualTo(3);
        assertThat(result.evaluationSummary().evaluatedAssets()).isZero();
    }

    @Test
    @DisplayName("Missing findings key / null policy results surface as error")
    void nullPolicyResultsAreError() {
        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(
                        SERVICE, POLICY, new ComplianceCheckResultDTO(null, true), LEVELS, 2, 2);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.ERROR);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.error()).isTrue();
        assertThat(result.evaluationSummary().totalCryptographicAssets()).isEqualTo(2);
    }

    @Test
    @DisplayName("Findings must identify expected assets to establish full coverage")
    void findingsMustMatchExpectedAssets() {
        ComplianceCheckResultDTO dto =
                new ComplianceCheckResultDTO(
                        List.of(
                                new BasicCryptographicAssetPolicyResult(
                                        "out-of-scope", SAFE, "ignored"),
                                new BasicCryptographicAssetPolicyResult(
                                        "expected-a", SAFE, "valid"),
                                new BasicCryptographicAssetPolicyResult(null, SAFE, "no identity")),
                        false);
        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(
                        SERVICE, POLICY, dto, LEVELS, 2, Set.of("expected-a", "expected-b"));
        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.PARTIAL);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().evaluatedAssets()).isEqualTo(1);
    }

    @Test
    @DisplayName("Not-applicable findings do not conceal missing expected assets")
    void notApplicableWithMissingAssetIsPartial() {
        ComplianceResult result =
                ComplianceResultFactory.fromFindings(
                        SERVICE,
                        POLICY,
                        List.of(new ComplianceFinding("aes-1", 4, "symmetric")),
                        LEVELS,
                        2,
                        Set.of("aes-1", "ml-kem-1"));
        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.PARTIAL);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().totalCryptographicAssets()).isEqualTo(2);
    }

    @Test
    @DisplayName("Service error flag yields evaluationStatus=error")
    void serviceError() {
        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(
                        SERVICE,
                        POLICY,
                        new ComplianceCheckResultDTO(List.of(), true),
                        LEVELS,
                        2,
                        1);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.ERROR);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.error()).isTrue();
    }

    @Test
    @DisplayName("Multiple OPA rules for one bom-ref count as one evaluated asset")
    void duplicateRuleFindingsDoNotInflateCoverage() {
        ComplianceCheckResultDTO dto =
                new ComplianceCheckResultDTO(
                        List.of(
                                new BasicCryptographicAssetPolicyResult(
                                        "ml-kem-1", SAFE, "safe by name"),
                                new BasicCryptographicAssetPolicyResult(
                                        "ml-kem-1", SAFE, "safe by OID")),
                        false);

        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(SERVICE, POLICY, dto, LEVELS, 2, 2);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.PARTIAL);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().evaluatedAssets()).isEqualTo(1);
        assertThat(result.evaluationSummary().quantumSafeAssets()).isEqualTo(1);
    }

    @Test
    @DisplayName("A vulnerable OPA rule wins over a safe rule for the same bom-ref")
    void conflictingRuleFindingsFailClosedPerAsset() {
        ComplianceCheckResultDTO dto =
                new ComplianceCheckResultDTO(
                        List.of(
                                new BasicCryptographicAssetPolicyResult(
                                        "ambiguous-1", SAFE, "safe by name"),
                                new BasicCryptographicAssetPolicyResult(
                                        "ambiguous-1", VULNERABLE, "not in the OID whitelist")),
                        false);

        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(SERVICE, POLICY, dto, LEVELS, 2, 1);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().evaluatedAssets()).isEqualTo(1);
        assertThat(result.evaluationSummary().quantumSafeAssets()).isZero();
        assertThat(result.evaluationSummary().vulnerableAssets()).isEqualTo(1);
    }

    @Test
    @DisplayName("Empty CBOM is not_applicable, not quantum-safe")
    void emptyCbomIsNotApplicable() {
        ComplianceResult result =
                ComplianceResultFactory.fromFindings(SERVICE, POLICY, List.of(), LEVELS, 2, 0);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.NOT_APPLICABLE);
        assertThat(result.globalComplianceStatus()).isFalse();
    }

    @Test
    @DisplayName("Symmetric-only / NA-only findings are not_applicable")
    void notApplicableOnly() {
        List<ComplianceFinding> findings =
                List.of(
                        new ComplianceFinding("aes-1", 4, "symmetric"),
                        new ComplianceFinding("aes-2", 4, "symmetric"));

        ComplianceResult result =
                ComplianceResultFactory.fromFindings(SERVICE, POLICY, findings, LEVELS, 2, 2);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.NOT_APPLICABLE);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().notApplicableAssets()).isEqualTo(2);
        assertThat(result.evaluationSummary().applicableAssets()).isZero();
    }

    @Test
    @DisplayName("Incomplete finding coverage is partial, never quantum-safe")
    void incompleteCoverageIsPartial() {
        List<ComplianceFinding> findings = List.of(new ComplianceFinding("ml-kem-1", 3, "safe"));

        ComplianceResult result =
                ComplianceResultFactory.fromFindings(SERVICE, POLICY, findings, LEVELS, 2, 3);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.PARTIAL);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().evaluatedAssets()).isEqualTo(1);
        assertThat(result.evaluationSummary().totalCryptographicAssets()).isEqualTo(3);
    }

    @Test
    @DisplayName("Unknown asymmetric algorithm is evaluated but not quantum-safe")
    void unknownAssetBlocksGlobalSafe() {
        List<ComplianceFinding> findings =
                List.of(
                        new ComplianceFinding("rsa-1", 2, "unknown"),
                        new ComplianceFinding("aes-1", 4, "na"));

        ComplianceResult result =
                ComplianceResultFactory.fromFindings(SERVICE, POLICY, findings, LEVELS, 2, 2);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().unknownAssets()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mixed safe, vulnerable, and unknown is evaluated and not quantum-safe")
    void mixedAssetsNotGloballySafe() {
        List<ComplianceFinding> findings =
                List.of(
                        new ComplianceFinding("ml-kem-1", 3, "safe"),
                        new ComplianceFinding("rsa-1", 1, "vulnerable"),
                        new ComplianceFinding("weird-1", 2, "unknown"));

        ComplianceResult result =
                ComplianceResultFactory.fromFindings(SERVICE, POLICY, findings, LEVELS, 2, 3);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationSummary().quantumSafeAssets()).isEqualTo(1);
        assertThat(result.evaluationSummary().vulnerableAssets()).isEqualTo(1);
        assertThat(result.evaluationSummary().unknownAssets()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fully evaluated quantum-safe CBOM is the positive control")
    void fullyEvaluatedQuantumSafe() {
        List<ComplianceFinding> findings =
                List.of(
                        new ComplianceFinding("ml-kem-1", 3, "safe"),
                        new ComplianceFinding("aes-1", 4, "na"));

        ComplianceResult result =
                ComplianceResultFactory.fromFindings(SERVICE, POLICY, findings, LEVELS, 2, 2);

        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(result.globalComplianceStatus()).isTrue();
        assertThat(result.error()).isFalse();
        assertThat(result.evaluationSummary().applicableAssets()).isEqualTo(1);
        assertThat(result.evaluationSummary().quantumSafeAssets()).isEqualTo(1);
    }

    @Test
    @DisplayName("Empty findings previously failed-open via noneMatch; factory must not")
    void emptyFindingsDoNotFailOpenViaNoneMatch() {
        // Historical bug: Stream.noneMatch on an empty stream returns true.
        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(
                        SERVICE,
                        POLICY,
                        new ComplianceCheckResultDTO(List.of(), false),
                        LEVELS,
                        2,
                        5);

        assertThat(result.globalComplianceStatus()).isFalse();
        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.NOT_EVALUATED);
    }

    @Test
    @DisplayName("Policy results map into findings with the same fail-closed verdict")
    void mapsPolicyResults() {
        ComplianceCheckResultDTO dto =
                new ComplianceCheckResultDTO(
                        List.of(
                                new BasicCryptographicAssetPolicyResult("ml-kem-1", SAFE, "ok"),
                                new BasicCryptographicAssetPolicyResult("aes-1", NA, "na")),
                        false);

        ComplianceResult result =
                ComplianceResultFactory.fromServiceResult(SERVICE, POLICY, dto, LEVELS, 2, 2);

        assertThat(result.complianceFindings()).hasSize(2);
        assertThat(result.globalComplianceStatus()).isTrue();
        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.EVALUATED);
    }
}
