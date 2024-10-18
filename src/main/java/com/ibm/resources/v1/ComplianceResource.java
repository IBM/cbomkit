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

import com.ibm.compliance.IComplianceService;
import com.ibm.configuration.IConfiguration;
import com.ibm.repository.IScanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/v1/compliance")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ComplianceResource {
    private static final Logger LOG = Logger.getLogger(ComplianceResource.class);

    @Nonnull private final IScanRepository cbomRepository;
    @Nullable private final IComplianceService complianceService;

    @Inject
    public ComplianceResource(@Nonnull final IConfiguration configuration) {
        this.cbomRepository = configuration.getCBOMRepository();
        this.complianceService = configuration.getComplianceService();
    }

    @POST
    @Path("/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Verify the compliance of a provided CBOM against a policy",
            description =
                    "Returns the JSON sent by the Regulator API, containing various information about the compliance of the CBOM for a set policy.")
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
                                                                "{"
                                                                        + "\"policyIdentifier\": \"ipsec\","
                                                                        + "\"policyDocumentTitle\": \"Some Policy Detailed Name\","
                                                                        + "\"policyDocumentVersion\": \"v0.4\","
                                                                        + "\"policyDocumentURL\": \"https://example.com/...\","
                                                                        + "\"compliant\": false,"
                                                                        + "\"findings\": []"
                                                                        + "}]"))),
                @APIResponse(
                        responseCode = "503",
                        description = "Not found",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        example =
                                                                "{"
                                                                        + "\"regulator_url\": \"some url\","
                                                                        + "\"unavailable\": true"
                                                                        + "}]")))
            })
    @Transactional
    public Response check(
            @Nullable String cbomString,
            @Nullable @QueryParam("policyIdentifier") String policyIdentifier) {
        LOG.info("Compliance /check");

        if (policyIdentifier == null) {
            LOG.info("Compliance /check: policyIdentifier is empty");
            return Response.status(403).build();
        }

        if (cbomString == null) {
            LOG.info("Compliance /check: cbom is empty");
            return Response.status(403).build();
        }

        return Optional.ofNullable(complianceService)
                .map(
                        service -> {
                            final String result =
                                    service.check(policyIdentifier, cbomString).toJson();
                            return Response.ok(result).build();
                        })
                .orElse(Response.status(501).build());
    }

    @GET
    @Path("/check/purl")
    @Operation(
            summary =
                    "Verify the compliance of a stored CBOM identified by it's PURL against a policy",
            description =
                    "Returns the JSON sent by the Regulator API, containing various information about the compliance of the CBOM for a set policy.")
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
                                                                "{"
                                                                        + "\"policyIdentifier\": \"quantum_safe\","
                                                                        + "\"policyDocumentTitle\": \"Some Policy Detailed Name\","
                                                                        + "\"policyDocumentVersion\": \"v0.4\","
                                                                        + "\"policyDocumentURL\": \"https://example.com/...\","
                                                                        + "\"compliant\": false,"
                                                                        + "\"findings\": []"
                                                                        + "}]"))),
                @APIResponse(
                        responseCode = "503",
                        description = "Not found",
                        content =
                                @Content(
                                        schema =
                                                @Schema(
                                                        example =
                                                                "{"
                                                                        + "\"regulator_url\": \"some url\","
                                                                        + "\"unavailable\": true"
                                                                        + "}]")))
            })
    @Transactional
    public Response checkPurl(
            @Nullable @RestQuery("purl") String purl,
            @Nonnull @RestQuery("cbomVersion") @DefaultValue("1.6") String cbomVersion,
            @Nullable @RestQuery("policyIdentifier") String policyIdentifier) {
        LOG.info("Compliance /checkPurl");

        if (policyIdentifier == null) {
            LOG.info("Compliance /check: policyIdentifier is empty");
            return Response.status(403).build();
        }

        if (purl == null) {
            LOG.info("Compliance /check: purl is empty");
            return Response.status(403).build();
        }

        return Optional.ofNullable(complianceService)
                .map(
                        service ->
                                cbomRepository
                                        .findByPurl(purl, cbomVersion)
                                        .map(
                                                scan ->
                                                        service.check(
                                                                policyIdentifier,
                                                                scan.getBom().toString()))
                                        .map(res -> Response.ok(res))
                                        .orElse(Response.status(404)))
                .orElse(Response.status(501))
                .build();
    }
}
