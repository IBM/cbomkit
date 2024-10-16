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
package com.ibm.compliance;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.component.crypto.AlgorithmProperties;
import org.cyclonedx.model.component.crypto.CryptoProperties;
import org.cyclonedx.model.component.crypto.enums.AssetType;
import org.cyclonedx.model.component.crypto.enums.Primitive;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.parsers.Parser;
import org.jboss.logging.Logger;

public class BasicQuantumSafeComplianceService implements IComplianceService {
    private static final Logger LOG = Logger.getLogger(BasicQuantumSafeComplianceService.class);

    private static final String COMPLIANCE_SERVICE_NAME = "Basic Backend Compliance Service";
    private static final String POLICY_NAME = "NIST Post-Quantum Cryptography";
    private static final List<Primitive> ASYMMETRIC_PRIMITIVES =
            Arrays.asList(Primitive.SIGNATURE, Primitive.KEY_AGREE, Primitive.KEM, Primitive.PKE);
    private static final List<Primitive> UNKNOWN_PRIMITIVES =
            Arrays.asList(Primitive.UNKNOWN, Primitive.OTHER);
    private static final List<String> WHITELIST_NAMES =
            Arrays.asList(
                    "ml-kem",
                    "ml-dsa",
                    "slh-dsa",
                    "pqxdh",
                    "bike",
                    "mceliece",
                    "frodokem",
                    "hqc",
                    "kyber",
                    "ntru",
                    "crystals",
                    "falcon",
                    "mayo",
                    "sphincs",
                    "xmss",
                    "lms");
    private static final List<String> WHITELIST_OIDS =
            Arrays.asList(
                    "1.3.6.1.4.1.2.267.12.4.4",
                    "1.3.6.1.4.1.2.267.12.6.5",
                    "1.3.6.1.4.1.2.267.12.8.7",
                    "1.3.9999.6.4.16",
                    "1.3.9999.6.7.16",
                    "1.3.9999.6.4.13",
                    "1.3.9999.6.7.13",
                    "1.3.9999.6.5.12",
                    "1.3.9999.6.8.12",
                    "1.3.9999.6.5.10",
                    "1.3.9999.6.8.10",
                    "1.3.9999.6.6.12",
                    "1.3.9999.6.9.12",
                    "1.3.9999.6.6.10",
                    "1.3.9999.6.9.10",
                    "1.3.6.1.4.1.22554.5.6.1",
                    "1.3.6.1.4.1.22554.5.6.2",
                    "1.3.6.1.4.1.22554.5.6.3");

    final List<ComplianceFormat.ComplianceLevel> complianceLevels = new LinkedList<>();

    public BasicQuantumSafeComplianceService() {
        complianceLevels.add(
                new ComplianceFormat.ComplianceLevel(
                        1, "Not Quantum Safe", "#fac532", ComplianceFormat.ComplianceIcon.WARNING));
        complianceLevels.add(
                new ComplianceFormat.ComplianceLevel(
                        2,
                        "Unknown",
                        "Unknown Compliance",
                        "#17a9d1",
                        ComplianceFormat.ComplianceIcon.UNKNOWN));
        complianceLevels.add(
                new ComplianceFormat.ComplianceLevel(
                        3,
                        "Quantum Safe",
                        "green",
                        ComplianceFormat.ComplianceIcon.CHECKMARK_SECURE));
        complianceLevels.add(
                new ComplianceFormat.ComplianceLevel(
                        4,
                        "Not Applicable",
                        "Not Applicable: we only categorize asymmetric algorithms",
                        "gray",
                        ComplianceFormat.ComplianceIcon.NOT_APPLICABLE));
    }

    @Nonnull
    public ComplianceFormat check(@Nonnull String policyIdentifier, @Nonnull String cbomString) {
        if (!policyIdentifier.equals(Policies.QUANTUM_SAFE.toString())) {
            LOG.warn(
                    this.getClass().getSimpleName()
                            + ": service only handles policy with id '"
                            + Policies.QUANTUM_SAFE
                            + "'");
            return ComplianceFormat.error(COMPLIANCE_SERVICE_NAME);
        }

        byte[] bomBytes = cbomString.getBytes(StandardCharsets.UTF_8);
        try {
            // Create the parser
            Parser parser = BomParserFactory.createParser(bomBytes);
            // Parse the BOM content
            Bom bom = parser.parse(bomBytes);
            // List storing all the findings
            final List<ComplianceFormat.ComplianceFinding> findings = new LinkedList<>();
            bom.getComponents().stream()
                    .filter(
                            component ->
                                    component.getBomRef() != null
                                            && component.getType() == Type.CRYPTOGRAPHIC_ASSET
                                            && component.getCryptoProperties() != null
                                            && component.getCryptoProperties().getAssetType()
                                                    == AssetType.ALGORITHM)
                    .forEach(
                            component -> {
                                final ComplianceFormat.ComplianceFinding finding =
                                        getCompliance(component);
                                logCompliance(component, finding);
                                findings.add(finding);
                            });
            boolean globalComplianceStatus =
                    findings.stream()
                            .noneMatch(finding -> finding.levelId == 1 || finding.levelId == 2);
            return new ComplianceFormat(
                    COMPLIANCE_SERVICE_NAME,
                    POLICY_NAME,
                    findings,
                    complianceLevels,
                    2,
                    globalComplianceStatus);
        } catch (ParseException e) {
            return ComplianceFormat.error(COMPLIANCE_SERVICE_NAME);
        }
    }

    @Nonnull
    private ComplianceFormat.ComplianceFinding getCompliance(@Nonnull Component component) {
        final String bomRef = component.getBomRef();
        final CryptoProperties cryptoProperties = component.getCryptoProperties();
        final AlgorithmProperties algorithmProperties = cryptoProperties.getAlgorithmProperties();
        if (algorithmProperties == null) {
            return new ComplianceFormat.ComplianceFinding(
                    bomRef,
                    2,
                    "The field 'algorithmProperties' was not set, which does not allow further categorization");
        }

        final Integer nistQuantumSecurityLevel = algorithmProperties.getNistQuantumSecurityLevel();
        if (nistQuantumSecurityLevel != null && nistQuantumSecurityLevel > 0) {
            return new ComplianceFormat.ComplianceFinding(
                    bomRef,
                    3,
                    "The field 'nistQuantumSecurityLevel' was set with a strictly positive value in the CBOM");
        }

        final Primitive primitive = algorithmProperties.getPrimitive();
        if (algorithmProperties.getPrimitive() == null) {
            return new ComplianceFormat.ComplianceFinding(
                    bomRef,
                    2,
                    "The asset primitive was not set, which does not allow further categorization");
        } else if (ASYMMETRIC_PRIMITIVES.contains(primitive)
                || UNKNOWN_PRIMITIVES.contains(primitive)) {
            final String name = component.getName();
            final String oid = cryptoProperties.getOid();
            if (oid != null && WHITELIST_OIDS.contains(oid)) {
                return new ComplianceFormat.ComplianceFinding(
                        bomRef,
                        3,
                        "The OID of the asset is part of the Quantum Safe OIDs whitelist");
            }
            if (name != null) {
                String lowerCaseName = name.toLowerCase();
                for (String whitelistItem : WHITELIST_NAMES) {
                    if (lowerCaseName.contains(whitelistItem)) {
                        return new ComplianceFormat.ComplianceFinding(
                                bomRef,
                                3,
                                "The name of the asset contains '"
                                        + whitelistItem
                                        + "', which is part of the Quantum Safe whitelist of component names");
                    }
                }
            }
            if (ASYMMETRIC_PRIMITIVES.contains(primitive)) {
                return new ComplianceFormat.ComplianceFinding(
                        bomRef,
                        1,
                        "The asset has an asymmetric primitive and does not match with the Quantum Safe whitelists of OIDs and names");
            } else {
                // Primitive is part of UNKNOWN_PRIMITIVES
                return new ComplianceFormat.ComplianceFinding(
                        bomRef,
                        2,
                        "The asset primitive is unclear and does not allow further categorization");
            }
        } else {
            return new ComplianceFormat.ComplianceFinding(
                    bomRef,
                    4,
                    "The asset has a symmetric primitive, so the Quantum Safe categorization is not applicable");
        }
    }

    private void logCompliance(
            @Nonnull Component component,
            @Nonnull ComplianceFormat.ComplianceFinding complianceFinding) {
        LOG.info(
                this.getClass().getSimpleName()
                        + ": ["
                        + component.getName()
                        + ", level: "
                        + complianceFinding.levelId
                        + "] - "
                        + complianceFinding.message);
    }
}
