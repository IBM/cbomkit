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

import com.ibm.configuration.IConfiguration;
import com.ibm.model.Scan;
import com.ibm.repository.IScanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/v1/cbom")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class CBOMResource {

    @Nonnull private final IScanRepository scanRepository;

    @Inject
    public CBOMResource(@Nonnull final IConfiguration configuration) {
        this.scanRepository = configuration.getCBOMRepository();
    }

    record Result(@Nonnull String purl, @Nonnull Scan result) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns a list of CBOMs related to the provided package urls",
            description = "Returns a list of stored CBOMs for each provided package url.")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "OK",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        example =
                                                                "[{"
                                                                        + "\"purl\": \"pkg:maven/commons-io/commons-io@2.11.0\","
                                                                        + "\"cbom\": {"
                                                                        + "\"id\": 251,"
                                                                        + "\"gitUrl\":  \"https://github.com/apache/commons-io\","
                                                                        + "\"branch\":  \"rel/commons-io-2.11.0\","
                                                                        + "\"cbomVersion\":  \"1.4-cbom-1.0\","
                                                                        + "\"createdAt\": \"2023-10-24T10:55:20.404+00:00\","
                                                                        + "\"bom: {}\""
                                                                        + "}"
                                                                        + "}]"))),
                @APIResponse(responseCode = "404", description = "Purl not found")
            })
    @Transactional
    public Response getCBOMs(
            @Nonnull @RestQuery List<String> purls,
            @Nonnull @RestQuery @DefaultValue("1.6") String cbomVersion) {
        List<Result> cbomList = new LinkedList<>();
        for (String purl : purls) {
            scanRepository
                    .findByPurl(purl, cbomVersion)
                    .ifPresent(cbom -> cbomList.add(new Result(purl, cbom)));
        }
        return Response.ok(cbomList).build();
    }

    @GET
    @Path("/lastn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Return recently generated CBOMs from the repository",
            description =
                    "Returns a list of the most recently generated CBOMs. "
                            + "The length of the list can by specified via the optional 'limit' "
                            + "parameter.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            example =
                                                    "[{"
                                                            + "\"id\": 251,"
                                                            + "\"gitUrl\": \"https://github.com/quarkusio/quarkus\","
                                                            + "\"branch\": \"main\","
                                                            + "\"cbomVersion\": \"1.4-cbom-1.0\","
                                                            + "\"createdAt\": \"2023-10-24T10:55:20.404+00:00\","
                                                            + "\"base64\": \"ewogICJib21Gb3JtY...\""
                                                            + "}]")))
    @Transactional
    public Response getLastCBOMs(@RestQuery @DefaultValue("5") int limit) {
        if (limit <= 0) {
            limit = 5;
        }
        List<Scan> scanList = scanRepository.getLastCBOMs(limit);
        return Response.ok(scanList).build();
    }

    @GET
    @Path("/algorithm/searchByName")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns the git URLs of all CBOMs that use the specified algorithm name.",
            description =
                    "Returns the git URLs of all CBOMs that use the specified algorithm name.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            example =
                                                    "[ \"https://github.com/quarkusio/quarkus\"]")))
    @Transactional
    public Response searchByAlgorithmName(
            @RestQuery("algorithm") String algorithm, @RestQuery @DefaultValue("5") int limit) {
        if (limit <= 0) {
            limit = 5;
        }
        final List<String> gitUrlList =
                scanRepository.findRepositoriesIncludingComponentWithAlgorithmName(
                        algorithm, limit);
        return Response.ok(gitUrlList).build();
    }

    @GET
    @Path("/algorithm/searchByOid")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Returns the git URLs of all CBOMs that use the specified algorithm.",
            description = "Returns the git URLs of all CBOMs that use the specified algorithm.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            example =
                                                    "[ \"https://github.com/quarkusio/quarkus\"]")))
    @Transactional
    public Response getGitByOID(
            @RestQuery("oid") String oid, @RestQuery @DefaultValue("5") int limit) {
        if (limit <= 0) {
            limit = 5;
        }
        final List<String> gitUrlList =
                scanRepository.findRepositoriesIncludingComponentWithOID(oid, limit);
        return Response.ok(gitUrlList).build();
    }
}
