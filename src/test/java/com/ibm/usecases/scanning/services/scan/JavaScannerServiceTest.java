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
import com.ibm.infrastructure.Configuration;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.usecases.scanning.services.indexing.JavaIndexService;
import com.ibm.usecases.scanning.services.indexing.ProjectModule;
import com.ibm.usecases.scanning.services.scan.java.JavaScannerService;
import com.ibm.utils.AssetableProgressDispatcher;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaScannerServiceTest {

    @Test
    void test() throws ClientDisconnected {
        final AssetableProgressDispatcher assetableProgressDispatcher =
                new AssetableProgressDispatcher();
        final Configuration configuration = new Configuration();

        final File projectDirectory = new File("src/test/testdata/java/keycloak");
        final JavaIndexService javaIndexService =
                new JavaIndexService(assetableProgressDispatcher, projectDirectory);
        // indexing
        final List<ProjectModule> projectModules = javaIndexService.index(null);
        assertThat(projectModules).hasSize(2);
        for (final ProjectModule projectModule : projectModules) {
            if (projectModule.identifier().equals("crypto/default")) {
                assertThat(projectModule.inputFileList()).hasSize(13);
            } else if (projectModule.identifier().equals("services")) {
                assertThat(projectModule.inputFileList()).hasSize(18);
            }
        }
        // scanning
        final JavaScannerService javaScannerService =
                new JavaScannerService(
                        assetableProgressDispatcher,
                        configuration.getJavaDependencyJARSPath(),
                        projectDirectory);
        javaScannerService.scan(
                new GitUrl("https://github.com/keycloak/keycloak"),
                new Revision("main"),
                new Commit("9c2825eb0e64aa7ea40b8dc3605d37046f6a24cb"),
                null,
                projectModules);
        // check
        assetableProgressDispatcher.hasNumberOfDetections(14);

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "AES",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                86))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "AES",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                119))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "key",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                132))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "EC",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                132))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "ConcatenationKDF",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                157))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "public-key",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                199))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "EC",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                199))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "ECDH",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCEcdhEsAlgorithmProvider.java",
                                208))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "public-key",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCECDSACryptoProvider.java",
                                80))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "EC",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/BCECDSACryptoProvider.java",
                                80))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "AES",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/AesKeyWrapAlgorithmProvider.java",
                                38))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "AES",
                                "src/test/testdata/java/keycloak/crypto/default/src/main/java/org/keycloak/crypto/def/AesKeyWrapAlgorithmProvider.java",
                                45))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "TLS",
                                "src/test/testdata/java/keycloak/services/src/main/java/org/keycloak/connections/httpclient/HttpClientBuilder.java",
                                234))
                .isTrue();

        assertThat(
                        assetableProgressDispatcher.hasDetectionWithNameAt(
                                "TLS",
                                "src/test/testdata/java/keycloak/services/src/main/java/org/keycloak/connections/httpclient/HttpClientBuilder.java",
                                245))
                .isTrue();
    }
}
