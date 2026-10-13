/*
 * CBOMkit
 * Copyright (C) 2026 PQCA
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
package com.ibm.infrastructure.compliance;

import com.fasterxml.jackson.annotation.JsonValue;
import javax.annotation.Nonnull;

/** Outcome of a compliance policy evaluation, distinct from the quantum-safe verdict. */
public enum EvaluationStatus {
    /** Policy ran and every applicable asset has a conclusive result. */
    EVALUATED("evaluated"),
    /** Policy ran but coverage is incomplete (some assets lack findings). */
    PARTIAL("partial"),
    /** Policy was missing, empty, or otherwise did not produce findings. */
    NOT_EVALUATED("not_evaluated"),
    /** CBOM has no applicable cryptographic assets for this policy. */
    NOT_APPLICABLE("not_applicable"),
    /** Evaluation failed (service error / malformed response). */
    ERROR("error");

    @Nonnull private final String value;

    EvaluationStatus(@Nonnull String value) {
        this.value = value;
    }

    @JsonValue
    @Nonnull
    public String value() {
        return value;
    }
}
