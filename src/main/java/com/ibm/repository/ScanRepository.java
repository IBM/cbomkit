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

import com.github.packageurl.PackageURL;
import com.ibm.model.IdentifiableScan;
import com.ibm.model.Scan;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class ScanRepository implements IScanRepository, PanacheRepository<Scan> {

    @Transactional
    public @Nonnull Optional<Scan> findByPurl(@Nonnull String purl, @Nullable String cbomVersion) {
        PanacheQuery<IdentifiableScan> query = IdentifiableScan.find("purl", purl);
        IdentifiableScan identifiableScan1 = query.firstResult();
        if (identifiableScan1 != null) {
            return identifiableScan1.getScans().stream()
                    .filter(scan -> Objects.equals(scan.getCbomSpecVersion(), cbomVersion))
                    .max(Comparator.comparing(Scan::getCreatedAt)); // latest match
        } else {
            return Optional.empty();
        }
    }

    @Transactional
    public @Nonnull Optional<Scan> findByPurl(
            @Nonnull PackageURL purl, @Nullable String cbomVersion) {
        return findByPurl(purl.toString(), cbomVersion);
    }

    @Transactional
    public @Nonnull List<Scan> getLastCBOMs(int limit) {
        Sort byCreatedAt = Sort.by("createdAt", Direction.Descending);
        PanacheQuery<Scan> query = Scan.findAll(byCreatedAt).page(Page.ofSize(limit));
        return query.firstPage().list();
    }

    @Transactional
    public @Nonnull List<String> findRepositoriesIncludingComponentWithAlgorithmName(
            @Nonnull String algorithm, int limit) {
        final String sql =
                "select distinct(gitUrl), createdAt from scan, jsonb_array_elements(bom->'components') c where c->>'name' = ? and c->'cryptoProperties'->>'assetType'='algorithm' order by createdAt desc limit ?";
        return getQuery(algorithm, limit, sql);
    }

    @Transactional
    public @Nonnull List<String> findRepositoriesIncludingComponentWithOID(
            @Nonnull String oid, int limit) {
        final String sql =
                "select distinct(gitUrl), createdAt from scan, jsonb_array_elements(bom->'components') c where c->'cryptoProperties'->>'oid' = ? and c->'cryptoProperties'->>'assetType'='algorithm' order by createdAt desc limit ?";
        return getQuery(oid, limit, sql);
    }

    private @Nonnull List<String> getQuery(String searchString, int limit, String sql) {
        Query query = Scan.getEntityManager().createNativeQuery(sql);
        query.setParameter(1, searchString);
        query.setParameter(2, limit);
        @SuppressWarnings("unchecked")
        List<Object[]> res = query.getResultList();
        return res.stream().map(x -> x[0].toString()).toList();
    }
}
