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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.ibm.DefaultTestConfiguration;
import com.ibm.cbom.TestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CBOMResourceTest extends TestBase {

    public CBOMResourceTest() {
        super(new DefaultTestConfiguration());
    }

    @Test
    @DisplayName("Test that /api/v1/cbom endpoint returns 200")
    void testCBOMEndpoint() {
        given().queryParams(
                        "purls",
                        this.testConfiguration.examplePURL(),
                        "cbomVersion",
                        this.testConfiguration.exampleCbomVersion())
                .when()
                .get("/api/v1/cbom")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Test that /api/v1/cbom/lastn endpoint returns up to 5 CBOMS")
    void testLastNEndpoint() {
        final int limit = 5;
        given().queryParam("limit", limit)
                .when()
                .get("/api/v1/cbom/lastn")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0), "size()", lessThanOrEqualTo(limit));
    }
}
