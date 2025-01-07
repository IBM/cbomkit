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
package com.ibm.usecases.scanning.services.depsdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ibm.usecases.scanning.errors.NoDataAvailableInDepsDevForPurl;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepsDevService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepsDevService.class);
    private static final String DEPS_DEV_URI = "https://api.deps.dev/v3alpha/purl/";
    private static final String SOURCE_REPO = "SOURCE_REPO";

    @Nonnull
    public String fetch(@Nonnull String purl) throws NoDataAvailableInDepsDevForPurl {
        LOGGER.info("Sending DepsDev request for " + purl);
        final HttpGet request =
                new HttpGet(DEPS_DEV_URI + URLEncoder.encode(purl, StandardCharsets.UTF_8));

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                final CloseableHttpResponse response = httpClient.execute(request); ) {
            if (response.getCode() != 200) {
                throw new NoDataAvailableInDepsDevForPurl(
                        purl, "bad status code: " + response.getCode());
            }
            final InputStream in = response.getEntity().getContent();
            return extractSourceRepo(in);
        } catch (IOException ioe) {
            throw new NoDataAvailableInDepsDevForPurl(purl, ioe.getMessage());
        }
    }

    @Nonnull
    public String extractSourceRepo(InputStream in) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(in);
        JsonNode version = rootNode.get("version");
        if (version != null) {
            ArrayNode links = (ArrayNode) version.get("links");
            if (links != null) {
                for (JsonNode link : links) {
                    if (SOURCE_REPO.equals(link.get("label").textValue())) {
                        return link.get("url").textValue();
                    }
                }
            }
        }
        throw new IOException();
    }
}
