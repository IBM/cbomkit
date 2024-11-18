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
package com.ibm.infrastructure.compliance.service;

import com.ibm.domain.compliance.CryptographicAsset;
import com.ibm.domain.compliance.PolicyIdentifier;
import com.ibm.infrastructure.compliance.ComplianceLevel;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cyclonedx.model.component.crypto.AlgorithmProperties;
import org.cyclonedx.model.component.crypto.CryptoProperties;
import org.cyclonedx.model.component.crypto.enums.Primitive;

public class BasicQuantumSafeComplianceService implements IComplianceService {
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

    @Nonnull private final Map<Integer, ComplianceLevel> complianceLevels;

    public BasicQuantumSafeComplianceService() {
        complianceLevels = new HashMap<>();
        complianceLevels.put(
                1,
                new ComplianceLevel(
                        1,
                        "Not Quantum Safe",
                        null,
                        "#fac532",
                        ComplianceLevel.ComplianceIcon.WARNING,
                        true));
        complianceLevels.put(
                2,
                new ComplianceLevel(
                        2,
                        "Unknown",
                        "Unknown Compliance",
                        "#17a9d1",
                        ComplianceLevel.ComplianceIcon.UNKNOWN,
                        true));
        complianceLevels.put(
                3,
                new ComplianceLevel(
                        3,
                        "Quantum Safe",
                        null,
                        "green",
                        ComplianceLevel.ComplianceIcon.CHECKMARK_SECURE,
                        false));
        complianceLevels.put(
                4,
                new ComplianceLevel(
                        4,
                        "Not Applicable",
                        "Not Applicable: we only categorize asymmetric algorithms",
                        "gray",
                        ComplianceLevel.ComplianceIcon.NOT_APPLICABLE,
                        false));
    }

    @Override
    public @Nonnull String getName() {
        return "Basic Backend Compliance Service";
    }

    @Override
    public @Nonnull List<ComplianceLevel> getComplianceLevels() {
        return new ArrayList<>(complianceLevels.values());
    }

    @Override
    public @Nonnull ComplianceLevel getDefaultComplianceLevel() {
        return this.complianceLevels.get(2);
    }

    @Override
    public @Nonnull ComplianceCheckResultDTO evaluate(
            @Nonnull PolicyIdentifier policyIdentifier,
            @Nonnull Collection<CryptographicAsset> cryptographicAssets) {
        if (!policyIdentifier.id().equals("quantum_safe")) {
            return new ComplianceCheckResultDTO(List.of(), true);
        }
        return new ComplianceCheckResultDTO(
                cryptographicAssets.stream().map(this::evaluate).toList(), false);
    }

    @SuppressWarnings("java:S3776")
    @Nonnull
    private ICryptographicAssetPolicyResult evaluate(
            @Nonnull CryptographicAsset cryptographicAsset) {
        final CryptoProperties cryptoProperties =
                cryptographicAsset.component().getCryptoProperties();
        final AlgorithmProperties algorithmProperties = cryptoProperties.getAlgorithmProperties();
        if (algorithmProperties == null) {
            return new BasicCryptographicAssetPolicyResult(
                    cryptographicAsset.identifier(),
                    this.complianceLevels.get(2),
                    "The field 'algorithmProperties' was not set, which does not allow further categorization");
        }

        final Integer nistQuantumSecurityLevel = algorithmProperties.getNistQuantumSecurityLevel();
        if (nistQuantumSecurityLevel != null && nistQuantumSecurityLevel > 0) {
            return new BasicCryptographicAssetPolicyResult(
                    cryptographicAsset.identifier(),
                    this.complianceLevels.get(3),
                    "The field 'nistQuantumSecurityLevel' was set with a strictly positive value in the CBOM");
        }

        final Primitive primitive = algorithmProperties.getPrimitive();
        if (algorithmProperties.getPrimitive() == null) {
            return new BasicCryptographicAssetPolicyResult(
                    cryptographicAsset.identifier(),
                    this.complianceLevels.get(2),
                    "The asset primitive was not set, which does not allow further categorization");
        } else if (ASYMMETRIC_PRIMITIVES.contains(primitive)
                || UNKNOWN_PRIMITIVES.contains(primitive)) {
            final String name = cryptographicAsset.component().getName();
            final String oid = cryptoProperties.getOid();
            if (oid != null && WHITELIST_OIDS.contains(oid)) {
                return new BasicCryptographicAssetPolicyResult(
                        cryptographicAsset.identifier(),
                        this.complianceLevels.get(3),
                        "The OID of the asset is part of the Quantum Safe OIDs whitelist");
            }
            if (name != null) {
                String lowerCaseName = name.toLowerCase();
                for (String whitelistItem : WHITELIST_NAMES) {
                    if (lowerCaseName.contains(whitelistItem)) {
                        return new BasicCryptographicAssetPolicyResult(
                                cryptographicAsset.identifier(),
                                this.complianceLevels.get(3),
                                "The name of the asset contains '"
                                        + whitelistItem
                                        + "', which is part of the Quantum Safe whitelist of component names");
                    }
                }
            }
            if (ASYMMETRIC_PRIMITIVES.contains(primitive)) {
                return new BasicCryptographicAssetPolicyResult(
                        cryptographicAsset.identifier(),
                        this.complianceLevels.get(1),
                        "The asset has an asymmetric primitive and does not match with the Quantum Safe whitelists of OIDs and names");
            } else {
                // Primitive is part of UNKNOWN_PRIMITIVES
                return new BasicCryptographicAssetPolicyResult(
                        cryptographicAsset.identifier(),
                        this.complianceLevels.get(2),
                        "The asset primitive is unclear and does not allow further categorization");
            }
        } else {
            return new BasicCryptographicAssetPolicyResult(
                    cryptographicAsset.identifier(),
                    this.complianceLevels.get(4),
                    "The asset has a symmetric primitive, so the Quantum Safe categorization is not applicable");
        }
    }
}
