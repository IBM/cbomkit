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
package com.ibm.model.api;

import java.util.List;

public class Message {

    public enum Type {
        DETECTION,
        LABEL,
        PURL,
        ERROR,
        CBOM,
        BRANCH,
        REVISION_HASH,
        SCANNED_FILE_COUNT,
        SCANNED_NUMBER_OF_LINES,
        SCANNED_DURATION
    }

    private Type type;

    private String message;

    private List<String> purls;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getPurls() {
        return purls;
    }

    public void setPurls(List<String> purls) {
        this.purls = purls;
    }
}
