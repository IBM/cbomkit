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
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepsDevService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepsDevService.class);
    private static final String DEPS_DEV_URI = "https://api.deps.dev/v3alpha/purl/";
    private static final String SOURCE_REPO = "SOURCE_REPO";

    private final class DepsDevResponseHandler implements HttpClientResponseHandler<String> {
        @Override
        public String handleResponse(ClassicHttpResponse httpResponse)
                throws ClientProtocolException, IOException {
            if (httpResponse.getCode() != HttpStatus.SC_OK) {
                return null;
            }
            return extractSourceRepo(httpResponse.getEntity().getContent());
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
            throw new IOException("Invalid deps.dev response");
        }
    }

    @Nonnull
    public String getSourceRepo(@Nonnull String purl) throws NoDataAvailableInDepsDevForPurl {
        LOGGER.info("Sending DepsDev request for " + purl);
        final HttpGet request =
                new HttpGet(DEPS_DEV_URI + URLEncoder.encode(purl, StandardCharsets.UTF_8));

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String srcRepo = httpClient.execute(request, new DepsDevResponseHandler());
            LOGGER.info("Source code repository: {}", srcRepo);
            return srcRepo;
        } catch (IOException ioe) {
            throw new NoDataAvailableInDepsDevForPurl(purl, ioe.getMessage());
        }
    }
}
