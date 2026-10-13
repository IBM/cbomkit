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
package com.ibm.infrastructure;

import com.ibm.infrastructure.compliance.IComplianceConfiguration;
import com.ibm.infrastructure.compliance.service.BasicQuantumSafeComplianceService;
import com.ibm.infrastructure.compliance.service.IComplianceService;
import com.ibm.infrastructure.compliance.service.opa.OPAComplianceService;
import com.ibm.infrastructure.compliance.service.opa.UnavailableOPAComplianceService;
import com.ibm.infrastructure.scanning.IScanConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Optional;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public final class Configuration implements IScanConfiguration, IComplianceConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static IComplianceService complianceService = null;

    @Nonnull
    @Override
    public IComplianceService getComplianceService() {
        if (complianceService != null) {
            return complianceService;
        }

        Optional<String> opaApiBase =
                ConfigProvider.getConfig()
                        .getOptionalValue("cbomkit.ext-policies.opa-api-base", String.class);
        if (opaApiBase.isPresent()) {
            try {
                complianceService = new OPAComplianceService();
                return complianceService;
            } catch (Exception e) {
                LOGGER.warn(
                        "Failed to connect to external compliance service: Open Policy Agent '{}'",
                        opaApiBase.get());
                complianceService = new UnavailableOPAComplianceService();
                return complianceService;
            }
        }
        complianceService = new BasicQuantumSafeComplianceService();
        return complianceService;
    }

    @Nonnull
    @Override
    public String getBaseCloneDirPath() {
        return ConfigProvider.getConfig()
                .getOptionalValue("cbomkit.clone-dir", String.class)
                .orElse(System.getProperty("user.home") + "/.cbomkit");
    }

    @Nonnull
    @Override
    public String getJavaDependencyJARSPath() {
        return ConfigProvider.getConfig()
                .getOptionalValue("cbomkit.scanning.java-jar-dir", String.class)
                .map(relativeDir -> new File(relativeDir).getAbsolutePath())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Could not load jar dependencies for java scanning")); // Error
    }
}
