/*
 * CBOMkit
 * Copyright (C) 2025 IBM
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
package com.ibm.usecases.scanning.services.scan;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.Revision;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.usecases.scanning.services.indexing.ProjectModule;
import com.ibm.usecases.scanning.services.indexing.PythonIndexService;
import com.ibm.usecases.scanning.services.scan.python.PythonScannerService;
import com.ibm.utils.AssetableProgressDispatcher;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class PythonScannerServiceTest {

    @Test
    void test() throws ClientDisconnected {
        final AssetableProgressDispatcher assetableProgressDispatcher =
                new AssetableProgressDispatcher();
        // indexing
        final File projectDirectory = new File("src/test/testdata/python/pyca");
        final PythonIndexService pythonIndexService =
                new PythonIndexService(assetableProgressDispatcher, projectDirectory);
        final List<ProjectModule> projectModules = pythonIndexService.index(null);
        assertThat(projectModules).hasSize(1);
        final ProjectModule projectModule = projectModules.getFirst();
        assertThat(projectModule.inputFileList()).hasSize(1);
        // scanning
        final PythonScannerService pythonScannerService =
                new PythonScannerService(assetableProgressDispatcher, projectDirectory);
        pythonScannerService.scan(
                new GitUrl("https://github.com/keycloak/keycloak"),
                new Revision("main"),
                new Commit("9c2825eb0e64aa7ea40b8dc3605d37046f6a24cb"),
                null,
                projectModules);
        // check
        assetableProgressDispatcher.hasNumberOfDetections(5);

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "SHA256", "src/test/testdata/python/pyca/generate_key.py", 4))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "AES128-CBC-PKCS7",
                                "src/test/testdata/python/pyca/generate_key.py",
                                4))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "HMAC-SHA256", "src/test/testdata/python/pyca/generate_key.py", 4))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "Fernet", "src/test/testdata/python/pyca/generate_key.py", 4))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "secret-key", "src/test/testdata/python/pyca/generate_key.py", 4))
                .isTrue();
    }
}
