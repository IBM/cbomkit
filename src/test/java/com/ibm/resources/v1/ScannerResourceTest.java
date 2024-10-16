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
package com.ibm.resources.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.DefaultTestConfiguration;
import com.ibm.cbom.TestBase;
import com.ibm.message.IMessageDispatcher;
import com.ibm.model.IdentifiableScan;
import com.ibm.model.IdentifiersInternal;
import com.ibm.model.Scan;
import com.ibm.model.api.Message;
import com.ibm.model.api.ScanRequest;
import com.ibm.repository.ScanRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ScannerResourceTest extends TestBase {

    public ScannerResourceTest() {
        super(new DefaultTestConfiguration());
    }

    @Test
    @DisplayName("Make sure that a git repo branch is cloned correctly")
    void testCloneGitRepo() {
        ScanRequest request =
                new ScanRequest(
                        this.testConfiguration.exampleGitUrl(),
                        this.testConfiguration.exampleGitBranch(),
                        null);
        Optional<ScannerResource.GitRepo> repo =
                ScannerResource.cloneGitRepo(
                        request,
                        new IMessageDispatcher() {
                            @Override
                            public void sendLabelMessage(@Nonnull String message)
                                    throws ScannerResource.CancelScanException {}

                            @Override
                            public void sendCBOMMessage(@Nonnull String message)
                                    throws ScannerResource.CancelScanException {}

                            @Override
                            public void sendPurlMessage(@Nonnull List<String> purls)
                                    throws ScannerResource.CancelScanException {}

                            @Override
                            public void sendErrorMessage(@Nonnull String error)
                                    throws ScannerResource.CancelScanException {}

                            @Override
                            public void sendMessage(@Nonnull Message.Type type, @Nonnull String str)
                                    throws ScannerResource.CancelScanException {}
                        });
        Assertions.assertTrue(repo.isPresent());
        Assertions.assertNotNull(repo.get().cloneDir());
        Assertions.assertNotNull(repo.get().commitHash());

        try (Git git = Git.open(repo.get().cloneDir())) {
            Assertions.assertTrue(git.status().call().isClean());
        } catch (IOException | GitAPIException e) {
            // ignore
        } finally {
            try {
                FileUtils.deleteDirectory(repo.get().cloneDir());
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Transactional
    @Test
    @DisplayName("Make sure that gitURL is correctly translated to purl")
    void testGetIdentifier() {
        final IdentifiersInternal ii =
                new IdentifiersInternal(
                        this.testConfiguration.exampleGitUrl(),
                        List.of(this.testConfiguration.examplePURL()));
        ScannerResource sr = new ScannerResource(this.testConfiguration);
        Optional<IdentifiersInternal> ids =
                sr.getIdentifier(this.testConfiguration.exampleGitUrl());
        Assertions.assertTrue(ids.isPresent());
        Assertions.assertEquals(ii.getPurls(), ids.get().getPurls());
    }

    @Inject ScanRepository scanRepository;

    @Transactional
    @Test
    @DisplayName("Make sure that CBOM is stored correcly")
    void testStoreCbom() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode cbomJson = mapper.readTree(this.testConfiguration.exampleCbomString());
        final IdentifiersInternal identifiers =
                new IdentifiersInternal(
                        this.testConfiguration.exampleGitUrl(),
                        List.of(this.testConfiguration.examplePURL()));

        ScanRequest request =
                new ScanRequest(
                        this.testConfiguration.exampleGitUrl(),
                        this.testConfiguration.exampleGitBranch(),
                        null);
        ScannerResource resource = new ScannerResource(this.testConfiguration);
        resource.storeCBOM(cbomJson, identifiers, request, "01abcdef");

        PanacheQuery<Scan> query =
                Scan.find("gitUrl = ?1 and branch = ?2", request.gitUrl(), request.branch());
        Scan scan = query.firstResult();
        Assertions.assertNotNull(scan);
        Assertions.assertEquals(request.gitUrl(), scan.getGitUrl());
        Assertions.assertEquals(request.branch(), scan.getBranch());
        Assertions.assertEquals(cbomJson, scan.getBom());

        IdentifiableScan.deleteAll();
        Scan.deleteAll();
    }
}
