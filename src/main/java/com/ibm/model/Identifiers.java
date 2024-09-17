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

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import java.util.List;

@Entity
@Cacheable
public final class Identifiers extends PanacheEntity {
    private String gitIdentifier;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> purls;

    public Identifiers(String gitIdentifier, List<String> purls) {
        this.gitIdentifier = gitIdentifier;
        this.purls = purls;
    }

    public Identifiers() {}

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
