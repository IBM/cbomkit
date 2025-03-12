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
package com.ibm.infrastructure.database.readmodels;

import app.bootstrap.core.ddd.IReadRepository;
import com.github.packageurl.PackageURL;
import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ICBOMReadRepository extends IReadRepository<UUID, CBOMReadModel> {

    /**
     * Returns latest CBOM related to the git url, commit and packageFolder.
     *
     * @param gitUrl the git url that identifies the CBOM.
     * @param commit the commit id (optional)
     * @param packageFolder the packageFolder (optional)
     * @return CBOM read model.
     */
    @Nonnull
    Optional<CBOMReadModel> findBy(
            @Nonnull GitUrl gitUrl, @Nullable Commit commit, @Nullable Path packageFolder);

    @Nonnull
    Optional<CBOMReadModel> findBy(@Nonnull PackageURL purl, @Nullable Commit commit);

    @Nonnull
    Optional<CBOMReadModel> findBy(@Nonnull String projectIdentifier);

    @Nonnull
    Collection<CBOMReadModel> getRecent(int limit);
}
