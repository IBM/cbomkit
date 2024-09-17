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
package com.ibm.model;

import java.util.List;

public final class IdentifiersInternal {

    private String gitIdentifier;

    private List<String> purls;

    public IdentifiersInternal(String gitIdentifier, List<String> purls) {
        this.gitIdentifier = gitIdentifier;
        this.purls = purls;
    }

    public IdentifiersInternal() {}

    public String getGitIdentifier() {
        return gitIdentifier;
    }

    public void setGitIdentifier(String gitIdentifier) {
        this.gitIdentifier = gitIdentifier;
    }

    public List<String> getPurls() {
        return purls;
    }

    public void setPurls(List<String> purls) {
        this.purls = purls;
    }
}
