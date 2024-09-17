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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ComplianceFormat {
    private final @Nonnull String complianceServiceName;
    private @Nullable String policyName;
    private @Nullable List<ComplianceFinding> findings;
    private @Nullable List<ComplianceLevel> complianceLevels;
    private int defaultComplianceLevel;
    private boolean globalComplianceStatus;

    private boolean error = false;

    public ComplianceFormat(
            @Nonnull String complianceServiceName,
            @Nonnull String policyName,
            @Nonnull List<ComplianceFinding> findings,
            @Nonnull List<ComplianceLevel> complianceLevels,
            int defaultComplianceLevel,
            boolean globalComplianceStatus) {
        this.complianceServiceName = complianceServiceName;
        this.policyName = policyName;
        this.findings = findings;
        this.complianceLevels = complianceLevels;
        this.defaultComplianceLevel = defaultComplianceLevel;
        this.globalComplianceStatus = globalComplianceStatus;
    }

    // Constructor used to report an error by calling `ComplianceFormat.error()`
    private ComplianceFormat(@Nonnull String complianceServiceName) {
        this.complianceServiceName = complianceServiceName;
        this.error = true;
        // All other fields are null
    }

    @Nonnull
    public static ComplianceFormat error(@Nonnull String complianceServiceName) {
        return new ComplianceFormat(complianceServiceName);
    }

    public static class ComplianceFinding {
        // Fields are public to make them serializable by the ObjectMapper
        // without needing to write getters
        @Nonnull public String bomRef;
        public int levelId;
        @Nullable public String message; // optional

        public ComplianceFinding(@Nonnull String bomRef, int levelId) {
            this.bomRef = bomRef;
            this.levelId = levelId;
        }

        public ComplianceFinding(@Nonnull String bomRef, int levelId, @Nonnull String message) {
            this.bomRef = bomRef;
            this.levelId = levelId;
            this.message = message;
        }
    }

    public static class ComplianceLevel {
        // Fields are public to make them serializable by the ObjectMapper
        // without needing to write getters
        public int id;
        @Nonnull public String label;
        @Nullable public String description; // optional
        @Nonnull public String colorHex;

        @Nonnull public ComplianceIcon icon;

        public ComplianceLevel(
                int id,
                @Nonnull String label,
                @Nonnull String colorHex,
                @Nonnull ComplianceIcon icon) {
            this.id = id;
            this.label = label;
            this.colorHex = colorHex;
            this.icon = icon;
        }

        public ComplianceLevel(
                int id,
                @Nonnull String label,
                @Nonnull String description,
                @Nonnull String colorHex,
                @Nonnull ComplianceIcon icon) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.colorHex = colorHex;
            this.icon = icon;
        }
    }

    public enum ComplianceIcon {
        CHECKMARK,
        CHECKMARK_SECURE,
        WARNING,
        ERROR,
        NOT_APPLICABLE,
        UNKNOWN,
        TEST,
    }

    // Getters (necessary for ObjectMapper)
    public String getComplianceServiceName() {
        return complianceServiceName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public List<ComplianceFinding> getFindings() {
        return findings;
    }

    public boolean getError() {
        return error;
    }

    public List<ComplianceLevel> getComplianceLevels() {
        return complianceLevels;
    }

    public int getDefaultComplianceLevel() {
        return defaultComplianceLevel;
    }

    public boolean getGlobalComplianceStatus() {
        return globalComplianceStatus;
    }

    // Export this object to Json
    public String toJson() {
        // Exports only public fields or fields with public getters
        ObjectMapper mapper = new ObjectMapper();
        // Exports only non null fields
        mapper.setSerializationInclusion(Include.NON_NULL);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
