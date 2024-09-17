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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.model.IdentifiableScan;
import com.ibm.model.api.ScanRequest;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.component.evidence.Occurrence;

public final class Utils {

    private Utils() {
        // nothing
    }

    public static void sanitizeOccurrence(final File clonedProjectDir, Component component) {
        List<Occurrence> occurrenceList =
                Optional.ofNullable(component.getEvidence())
                        .map(Evidence::getOccurrences)
                        .orElse(Collections.emptyList());

        if (occurrenceList.isEmpty()) {
            return;
        }

        final String baseDirPath = clonedProjectDir.getAbsolutePath();
        occurrenceList.forEach(
                occurrence -> {
                    if (occurrence.getLocation().startsWith(baseDirPath)) {
                        occurrence.setLocation(
                                occurrence.getLocation().substring(baseDirPath.length() + 1));
                    }
                });
    }

    private static ArrayNode getOrCreateProperties(JsonNode root) {
        JsonNode metadata = root.path("metadata");
        if (metadata.isMissingNode()) {
            metadata = ((ObjectNode) root).putObject("metadata");
        }
        JsonNode properties = metadata.path("properties");
        if (!properties.isArray()) {
            properties = ((ObjectNode) metadata).putArray("properties");
        }
        return (ArrayNode) properties;
    }

    public static void addProperties(
            JsonNode root,
            ScanRequest request,
            String commitHash,
            List<IdentifiableScan> identifiableScans) {
        ArrayNode properties = getOrCreateProperties(root);

        // add git-url
        ObjectNode gitLink = JsonNodeFactory.instance.objectNode();
        gitLink.put("name", "git-url").put("value", request.gitUrl());
        properties.add(gitLink);

        // add git-branch
        ObjectNode gitBranch = JsonNodeFactory.instance.objectNode();
        gitBranch.put("name", "git-branch").put("value", request.branch());
        properties.add(gitBranch);

        // add git-subfolder
        if (request.subfolder() != null) {
            ObjectNode gitSubFolder = JsonNodeFactory.instance.objectNode();
            gitSubFolder.put("name", "git-subfolder").put("value", request.subfolder());
            properties.add(gitSubFolder);
        }

        // add commit
        ObjectNode commit = JsonNodeFactory.instance.objectNode();
        commit.put("name", "commit").put("value", commitHash);
        properties.add(commit);

        // add purls
        for (IdentifiableScan identifiableScan : identifiableScans) {
            ObjectNode purlProp = JsonNodeFactory.instance.objectNode();
            purlProp.put("name", "purl").put("value", identifiableScan.getPurl());
            properties.add(purlProp);
        }
    }
}
