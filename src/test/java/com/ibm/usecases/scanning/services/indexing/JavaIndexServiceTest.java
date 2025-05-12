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
package com.ibm.usecases.scanning.services.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;

class JavaIndexServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaIndexServiceTest.class);

    @Test
    void test() throws ClientDisconnected {
        final IProgressDispatcher progressDispatcher =
                progressMessage -> LOGGER.info(progressMessage.toString());

        final JavaIndexService javaIndexService =
                new JavaIndexService(
                        progressDispatcher, new File("src/test/testdata/java/keycloak"));
        final List<ProjectModule> projectModules = javaIndexService.index(null);
        assertThat(projectModules).hasSize(2);
        for (ProjectModule projectModule : projectModules) {
            if (projectModule.identifier().equals("crypto/default")) {
                assertThat(projectModule.inputFileList()).hasSize(13);
            } else if (projectModule.identifier().equals("services")) {
                assertThat(projectModule.inputFileList()).hasSize(18);
            }
        }
    }

    @Test
    void plain() throws ClientDisconnected {
        final IProgressDispatcher progressDispatcher =
                progressMessage -> LOGGER.info(progressMessage.toString());

        final JavaIndexService javaIndexService =
                new JavaIndexService(progressDispatcher, new File("src/test/testdata/java/plain"));
        final List<ProjectModule> projectModules = javaIndexService.index(null);
        assertThat(projectModules).hasSize(1);
        final ProjectModule projectModule = projectModules.getFirst();
        assertThat(projectModule.inputFileList()).hasSize(1);
    }

    @Test
    void nested() throws ClientDisconnected {
        final IProgressDispatcher progressDispatcher =
                progressMessage -> LOGGER.info(progressMessage.toString());

        final JavaIndexService javaIndexService =
                new JavaIndexService(progressDispatcher, new File("src/test/testdata/java/nested"));
        final List<ProjectModule> projectModules = javaIndexService.index(null);
        assertThat(projectModules).hasSize(2);
        final ProjectModule projectModule1 = projectModules.getFirst();
        List<InputFile> inputFiles1 = projectModule1.inputFileList();
        assertThat(inputFiles1).hasSize(1);
        assertThat(inputFiles1.getFirst().filename()).isEqualTo("JavaCryptoInModule.java");

        final ProjectModule projectModule2 = projectModules.getLast();
        List<InputFile> inputFiles2 = projectModule2.inputFileList();
        assertThat(inputFiles2).hasSize(1);
        assertThat(inputFiles2.getFirst().filename()).isEqualTo("JavaCrypto.java");
    }
}
