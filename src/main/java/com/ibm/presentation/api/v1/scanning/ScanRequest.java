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
package com.ibm.presentation.api.v1.scanning;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ScanRequest {
    private @Nonnull String scanUrl;
    private @Nullable String branch;
    private @Nullable String subfolder;
    private @Nullable Credentials credentials;

    protected ScanRequest() {}

    public ScanRequest(
            @Nonnull @JsonProperty("scanUrl") String scanUrl,
            @Nullable @JsonProperty("branch") String branch,
            @Nullable @JsonProperty("subfolder") String subfolder,
            @Nullable @JsonProperty("credentials") Credentials credentials) {
        this.scanUrl = scanUrl;
        this.branch = branch;
        this.subfolder = subfolder;
        this.credentials = credentials;
    }

    public String getScanUrl() {
        return scanUrl;
    }

    @Nullable public String getBranch() {
        return branch;
    }

    @Nullable public String getSubfolder() {
        return subfolder;
    }

    @Nullable public Credentials getCredentials() {
        return credentials;
    }
}
