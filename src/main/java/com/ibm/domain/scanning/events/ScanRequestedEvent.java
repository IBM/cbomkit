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
package com.ibm.domain.scanning.events;

import app.bootstrap.core.ddd.DomainEvent;
import com.ibm.domain.scanning.ScanId;
import com.ibm.domain.scanning.authentication.ICredentials;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

public final class ScanRequestedEvent extends DomainEvent {
    @Nonnull private final ScanId scanId;
    @Nullable private final ICredentials credentials;

    public ScanRequestedEvent(@Nonnull ScanId scanId, @Nullable ICredentials credentials) {
        this.scanId = scanId;
        this.credentials = credentials;
    }

    @Nonnull
    public ScanId getScanId() {
        return scanId;
    }

    @Nullable public ICredentials getCredentials() {
        return credentials;
    }

    @Nonnull
    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "[scanId="
                + scanId
                + ", credentials="
                + Optional.ofNullable(credentials)
                        .map(c -> c.getClass().getSimpleName())
                        .orElse("none")
                + "]";
    }
}
