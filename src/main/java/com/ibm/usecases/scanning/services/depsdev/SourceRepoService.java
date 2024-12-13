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
import com.ibm.usecases.scanning.errors.GetSourceRepoFailed;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;

public class SourceRepoService {
    public SourceRepoService() {}

    private static final Logger LOGGER = Logger.getLogger(SourceRepoService.class);
    private static final String DEPS_DEV_URI = "https://api.deps.dev/v3alpha/purl/";
    private static final String SOURCE_REPO = "SOURCE_REPO";

    @Nullable public String fetch(@Nonnull String purl) throws GetSourceRepoFailed {
        LOGGER.info("Sending DepsDev request for " + purl);
        final HttpGet request =
                new HttpGet(DEPS_DEV_URI + URLEncoder.encode(purl, StandardCharsets.UTF_8));
        // final RequestConfig requestConfig =
        //         RequestConfig.custom().setSocketTimeout(600 * 1000).build();
        // request.setConfig(requestConfig);

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                final CloseableHttpResponse response = httpClient.execute(request); ) {
            final StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == 200) {
                InputStream in = response.getEntity().getContent();
                return extractSourceRepo(in);
            } else {
                LOGGER.error("DepsDev response status:" + status.getStatusCode());
            }
        } catch (IOException ioe) {
            LOGGER.error("DepsDev request failed : " + ioe.getMessage());
            throw new GetSourceRepoFailed(ioe.getMessage());
        }
        throw new GetSourceRepoFailed("Could find soure repo for purl: " + purl);
    }

    @Nullable public String extractSourceRepo(InputStream in) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(in);
        JsonNode version = rootNode.get("version");
        if (version != null) {
            ArrayNode links = (ArrayNode) version.get("links");
            if (links != null) {
                Iterator<JsonNode> it = links.iterator();
                while (it.hasNext()) {
                    JsonNode link = it.next();
                    if (SOURCE_REPO.equals(link.get("label").textValue())) {
                        return link.get("url").textValue();
                    }
                }
            }
        }
        return null;
    }
}
