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
package com.ibm.domain.scanning;

import app.bootstrap.core.ddd.IValueObject;
import com.ibm.domain.scanning.errors.InvalidScanUrl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ResolvedScanRequest implements IValueObject {
    @Nullable ScanUrl gitUrl;
    @Nonnull ScanUrl scanUrl;
    @Nonnull Revision revision;
    @Nullable String subFolder;

    public ScanUrl getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(ScanUrl gitUrl) {
        this.gitUrl = gitUrl;
    }

    public ScanUrl getScanUrl() {
        return scanUrl;
    }

    public Revision getRevision() {
        return revision;
    }

    public void setRevision(Revision revision) {
        this.revision = revision;
    }

    public String getSubFolder() {
        return subFolder;
    }

    @Override
    public void validate() throws InvalidScanUrl {
        scanUrl.validate();
        if (gitUrl != null) {
            gitUrl.validate();
        }
    }

    public ResolvedScanRequest(ScanRequest scanRequest) {
        this.scanUrl = scanRequest.scanUrl();
        this.revision = scanRequest.revision();
        this.subFolder = scanRequest.subFolder();
    }
}
