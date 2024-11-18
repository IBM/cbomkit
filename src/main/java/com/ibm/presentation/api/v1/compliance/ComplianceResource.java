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
package com.ibm.presentation.api.v1.compliance;

import app.bootstrap.core.cqrs.IQueryBus;
import com.ibm.usecases.compliance.queries.RequestComplianceCheckForCBOMQuery;
import com.ibm.usecases.compliance.queries.RequestComplianceCheckForScannedGitRepositoryQuery;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/api/v1/compliance")
@ApplicationScoped
public class ComplianceResource {

    @Nonnull protected final IQueryBus queryBus;

    public ComplianceResource(@Nonnull IQueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GET
    @Path("/check")
    @Operation(
            summary =
                    "Verify the compliance of a stored CBOM identified by it's PURL against a policy",
            description =
                    "Returns the JSON sent by the Regulator API, containing various information about the compliance of the CBOM for a set policy.")
    public Response checkStored(
            @Nullable @RestQuery("policyIdentifier") String policyIdentifier,
            @Nullable @RestQuery("gitUrl") String gitUrl,
            @Nullable @RestQuery("commit") String commit)
            throws ExecutionException, InterruptedException {
        if (policyIdentifier == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (gitUrl == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return this.queryBus
                .send(
                        new RequestComplianceCheckForScannedGitRepositoryQuery(
                                policyIdentifier, gitUrl, commit))
                .thenApply(res -> Response.ok(res).build())
                .get();
    }

    @POST
    @Path("/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Verify the compliance of a provided CBOM against a policy",
            description =
                    "Returns the JSON sent by the Regulator API, containing various information about the compliance of the CBOM for a set policy.")
    public Response check(
            @Nullable @QueryParam("policyIdentifier") String policyIdentifier,
            @Nullable String cbomString)
            throws ExecutionException, InterruptedException {
        if (policyIdentifier == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (cbomString == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return this.queryBus
                .send(new RequestComplianceCheckForCBOMQuery(policyIdentifier, cbomString))
                .thenApply(res -> Response.ok(res).build())
                .get();
    }
}
