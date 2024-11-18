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
package com.ibm.infrastructure;

import com.ibm.infrastructure.compliance.IComplianceConfiguration;
import com.ibm.infrastructure.compliance.service.BasicQuantumSafeComplianceService;
import com.ibm.infrastructure.compliance.service.IComplianceService;
import com.ibm.infrastructure.scanning.IScanConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public final class Configuration implements IScanConfiguration, IComplianceConfiguration {

    @Nonnull
    @Override
    public IComplianceService getComplianceService() {
        return new BasicQuantumSafeComplianceService();
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
    public List<File> getJavaDependencyJARS() {
        return ConfigProvider.getConfig()
                .getOptionalValue("cbomkit.scanning.java-jar-dir", String.class)
                .flatMap(Configuration::getJarFiles)
                .map(files -> Arrays.stream(files).toList())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Could not load jar dependencies for java scanning")); // Error
    }

    @Nonnull
    private static Optional<File[]> getJarFiles(@Nonnull String directoryPath) {
        final File directory = new File(directoryPath);
        final FileFilter jarFilter =
                file -> file.isFile() && file.getName().toLowerCase().endsWith(".jar");
        return Optional.ofNullable(directory.listFiles(jarFilter));
    }
}
