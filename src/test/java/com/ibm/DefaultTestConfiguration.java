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
package com.ibm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.compliance.IComplianceService;
import com.ibm.model.Scan;
import com.ibm.repository.IScanRepository;
import com.ibm.repository.ScanRepository;
import com.ibm.scan.IScanner;
import com.ibm.scan.IScannerManager;
import com.ibm.scan.JavaScanner;
import com.ibm.scan.PythonScanner;
import com.ibm.scan.ScannerManager;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.microprofile.config.ConfigProvider;

@Mock
@ApplicationScoped
public class DefaultTestConfiguration implements ITestConfiguration {
    @Nonnull
    @Override
    public String exampleCbomVersion() {
        return "1.6";
    }

    @Nonnull
    @Override
    public String exampleCbomString() {
        return "{\"cbom\":\"The cbom\"}";
    }

    @Nonnull
    @Override
    public Scan exampleCbom() {
        try {
            Scan scan = new Scan();
            scan.setCbomSpecVersion(this.exampleCbomVersion());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cbomJson = mapper.readTree(this.exampleCbomString());
            scan.setBom(cbomJson);
            scan.setGitUrl(this.exampleGitUrl());
            scan.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            return scan;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String exampleGitUrl() {
        return "https://github.com/apache/commons-io";
    }

    @Nonnull
    @Override
    public String exampleGitBranch() {
        return "master";
    }

    @Nonnull
    @Override
    public String examplePURL() {
        return "pkg:github/apache/commons-io";
    }

    @Nullable @Override
    public IComplianceService getComplianceService() {
        throw new UnsupportedOperationException("Override this function in your test class");
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
