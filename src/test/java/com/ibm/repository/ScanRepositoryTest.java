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
package com.ibm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.DefaultTestConfiguration;
import com.ibm.cbom.TestBase;
import com.ibm.model.IdentifiableScan;
import com.ibm.model.IdentifiersInternal;
import com.ibm.model.Scan;
import com.ibm.resources.v1.ScannerResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ScanRepositoryTest extends TestBase {

    public ScanRepositoryTest() {
        super(new DefaultTestConfiguration());
    }

    @Test
    @TestTransaction
    @DisplayName("Make sure that findByPurl returns a CBOM")
    void testFindByPurl() {
        final Scan scan = this.testConfiguration.exampleCbom();
        scan.persist();

        ScannerResource sr = new ScannerResource(this.testConfiguration);
        Optional<IdentifiersInternal> possibleIdentifiers = sr.getIdentifier(scan.getGitUrl());
        List<IdentifiableScan> identifiableScans = new ArrayList<>();
        for (String purl : possibleIdentifiers.orElseThrow().getPurls()) {
            IdentifiableScan identifiableScan1 = new IdentifiableScan();
            identifiableScan1.addScan(scan);
            identifiableScan1.setPurl(purl);
            identifiableScans.add(identifiableScan1);
        }
        IdentifiableScan.persist(identifiableScans);

        Optional<Scan> c =
                this.testConfiguration
                        .getCBOMRepository()
                        .findByPurl(
                                this.testConfiguration.examplePURL(),
                                this.testConfiguration.exampleCbomVersion());
        assertThat(c).isPresent();
    }

    @Test
    @TestTransaction
    @DisplayName("Make sure that getLastCboms returns up to 5 CBOMS")
    void testGetLastCBOMs() {
        this.testConfiguration.exampleCbom().persist();

        int limit = 5;
        List<Scan> scans = this.testConfiguration.getCBOMRepository().getLastCBOMs(limit);
        assertThat(scans).hasSize(1);
    }
}
