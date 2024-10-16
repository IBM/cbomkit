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
package com.ibm.configuration;

import com.ibm.Utils;
import com.ibm.compliance.BasicQuantumSafeComplianceService;
import com.ibm.compliance.IComplianceService;
import com.ibm.compliance.ibmregulator.IBMRegulatorClient;
import com.ibm.compliance.ibmregulator.IBMRegulatorService;
import com.ibm.repository.IScanRepository;
import com.ibm.repository.ScanRepository;
import com.ibm.scan.IBMqsScanner;
import com.ibm.scan.IScanner;
import com.ibm.scan.IScannerManager;
import com.ibm.scan.JavaScanner;
import com.ibm.scan.PythonScanner;
import com.ibm.scan.ScannerManager;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public final class Configuration implements IConfiguration {

    @Nullable @Override
    public IComplianceService getComplianceService() {
        if (ConfigProvider.getConfig()
                .getOptionalValue("service.compliance.regulator.enabled", Boolean.class)
                .orElse(false)) {
            // When the configuration enables the regulator, use the IBM Regulator Service
            IBMRegulatorClient service =
                    ConfigProvider.getConfig()
                            .getOptionalValue("service.compliance.regulator.url", String.class)
                            .map(
                                    api ->
                                            QuarkusRestClientBuilder.newBuilder()
                                                    .baseUri(URI.create(api))
                                                    .build(IBMRegulatorClient.class))
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Unable to get the configuration of the IBM Regulator client")); // Error
            return new IBMRegulatorService(service);
        } else {
            // Otherwise, use the basic compliance service
            return new BasicQuantumSafeComplianceService();
        }
    }

    @Nonnull
    @Override
    public IScanRepository getCBOMRepository() {
        return new ScanRepository();
    }

    @Nonnull
    @Override
    public IScannerManager getScannerManager() {
        // register scanners
        final List<IScanner> registry = new ArrayList<>();
        Optional<Boolean> useExplorer =
                ConfigProvider.getConfig()
                        .getOptionalValue(
                                "service.scanning.ibm-qs-explorer.enabled", Boolean.class);
        if (useExplorer.isPresent() && Boolean.TRUE.equals(useExplorer.get())) {
            ConfigProvider.getConfig()
                    .getOptionalValue("service.scanning.ibm-qs-explorer.url", String.class)
                    .ifPresent(api -> registry.add((new IBMqsScanner(api))));
            return new ScannerManager(registry);
        }
        registry.add((new JavaScanner(this)));
        registry.add((new PythonScanner()));
        return new ScannerManager(registry);
    }

    @Override
    public @Nonnull List<File> getJavaDependencyJARS() {
        return ConfigProvider.getConfig()
                .getOptionalValue("service.scanning.java-jar-dir", String.class)
                .flatMap(Utils::getJarFiles)
                .map(files -> Arrays.stream(files).toList())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Could not load jar dependencies for java scanning")); // Error
    }
}
