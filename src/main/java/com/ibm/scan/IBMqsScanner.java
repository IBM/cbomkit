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
package com.ibm.scan;

import static io.restassured.RestAssured.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.message.IMessageDispatcher;
import com.ibm.model.api.ScanRequest;
import com.ibm.resources.v1.ScannerResource.CancelScanException;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.jboss.logging.Logger;

public class IBMqsScanner extends AbstractScanner {
    private static final Logger LOG = Logger.getLogger(IBMqsScanner.class);
    private static final List<String> QEXTENSIONS = new ArrayList<>(List.of(".java", ".py"));

    private String scanId;
    private final String api;

    public IBMqsScanner(final String api) {
        this.api = api;
        LOG.info(
                "Successfully configured IBM qs explorer (api='"
                        + api
                        + "', languages='"
                        + QEXTENSIONS
                        + "')");
    }

    @Override
    public void init(
            @Nonnull IMessageDispatcher iMessageDispatcher,
            @Nonnull File clonedProject,
            @Nonnull ScanRequest request)
            throws CancelScanException {
        this.iMessageDispatcher = iMessageDispatcher;

        JsonArray ext = new JsonArray();
        ext.add(".java");
        ext.add(".py");

        JsonObject param = new JsonObject();
        param.add("extensions", ext);
        param.addProperty("rootFolderPath", clonedProject.getAbsolutePath());
        param.addProperty("scanType", "P");
        param.addProperty("enableLogging", true);

        if (request.subfolder() != null) {
            JsonArray includeDirs = new JsonArray();
            includeDirs.add(request.subfolder());
            param.add("pathInclusionFilter", includeDirs);
        }

        iMessageDispatcher.sendLabelMessage("Scanning repository " + clonedProject.getName());
        String prettyParam = param.toString();
        LOG.info("Scan params: " + param.toString());
        Response response =
                given().contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body(prettyParam)
                        .post(this.api + "/filesystem")
                        .then()
                        .contentType(ContentType.JSON)
                        .extract()
                        .response();
        this.scanId = response.jsonPath().getString("scanId");

        LOG.info("Scanning started, scanid=" + this.scanId);
    }

    @Override
    public IScanner.ScanResult scan() throws CancelScanException {
        IScanner.ScanResult scanResult = new IScanner.ScanResult();
        if (scanId == null) {
            LOG.error("Scanner not initialized");
            return scanResult;
        }

        long scanTimeStart = System.currentTimeMillis();
        while (!isDone()) {
            iMessageDispatcher.sendLabelMessage("Scan in progress ...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.error(e.getLocalizedMessage());
                Thread.currentThread().interrupt();
            }
        }

        scanResult.setStringBom(getCbom());
        scanResult.setDuration((System.currentTimeMillis() - scanTimeStart) / 1000);

        Response response =
                given().accept("*/*")
                        .get(this.api + "/" + this.scanId + "/report")
                        .then()
                        .contentType(ContentType.JSON)
                        .extract()
                        .response();

        JsonPath jsonPath = response.jsonPath();
        for (Object lines : jsonPath.getList("children.statistics.Lines_scanned")) {
            scanResult.addNumLines((Integer) lines);
        }
        for (Object files : jsonPath.getList("children.statistics.Files_scanned")) {
            scanResult.addNumFiles((Integer) files);
        }
        LOG.info("Retrieved statistics");

        return scanResult;
    }

    private boolean isDone() {
        Response response =
                given().accept("*/*")
                        .get(this.api + "/" + this.scanId + "/status")
                        .then()
                        .contentType(ContentType.JSON)
                        .extract()
                        .response();
        String status = response.jsonPath().getString("status");
        LOG.info("Scan status: " + status);
        return status.equals("DONE");
    }

    private String getCbom() {
        Response response =
                given().accept("*/*")
                        .get(this.api + "/" + this.scanId + "/cbom")
                        .then()
                        .contentType(ContentType.JSON)
                        .extract()
                        .response();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode bom = mapper.readTree(response.asInputStream());
            sanitizeFilePaths(bom);
            return bom.toString();
        } catch (IOException ioe) {
            LOG.error("Failed to sanitize BOM file paths");
            LOG.error(ioe.getLocalizedMessage());
        }

        // return response with original file paths
        return response.asString();
    }

    // Traverses the bom and sanitizes all filePath values
    // by trimming them at the first '@'.
    private void sanitizeFilePaths(@Nonnull JsonNode jsonNode) {
        if (jsonNode.isArray()) {
            for (JsonNode arrayItem : jsonNode) {
                sanitizeFilePaths(arrayItem);
            }
        } else if (jsonNode.isObject()) {
            Iterator<Entry<String, JsonNode>> it = jsonNode.fields();
            while (it.hasNext()) {
                Entry<String, JsonNode> e = it.next();
                String key = e.getKey();
                JsonNode element = e.getValue();
                if (key.equals("filePath") && element.isValueNode()) {
                    String filePath = element.asText();
                    int atIdx = filePath.indexOf('@');
                    if (atIdx >= 0) {
                        String sanitized = filePath.substring(0, atIdx);
                        ((ObjectNode) jsonNode).put(key, sanitized);
                    }
                }
                sanitizeFilePaths(e.getValue());
            }
        }
    }
}
