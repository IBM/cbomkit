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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nonnull;

@Entity
@Cacheable
public class IdentifiableScan extends PanacheEntity {
    @JsonProperty("purl")
    private String purl;

    @JsonProperty("scan")
    @ManyToMany
    private Collection<Scan> scan = new ArrayList<>();

    @Nonnull
    public String getPurl() {
        return purl;
    }

    public void setPurl(@Nonnull String purl) {
        this.purl = purl;
    }

    @Nonnull
    public Collection<Scan> getScans() {
        return scan;
    }

    public void addScan(@Nonnull Scan scan) {
        if (this.scan != null) {
            this.scan.add(scan);
        }
    }

    public void setScans(@Nonnull Collection<Scan> scans) {
        this.scan = scans;
    }
}
