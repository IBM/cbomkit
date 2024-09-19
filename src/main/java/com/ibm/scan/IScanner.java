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
package com.ibm.scan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.message.IMessageDispatcher;
import com.ibm.model.api.ScanRequest;
import com.ibm.resources.v1.ScannerResource.CancelScanException;
import java.io.File;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;

public interface IScanner {

    void init(
            @Nonnull IMessageDispatcher iMessageDispatcher,
            @Nonnull File clonedProject,
            @Nonnull ScanRequest request)
            throws CancelScanException;

    ScanResult scan() throws CancelScanException;

    class ScanResult {
        private int numFiles = 0;
        private int numLines = 0;
        private long duration = 0;
        private Bom bom = null;
        // This is a hack for integrating QSScanner.
        private String stringBom = null;

        public int getNumFiles() {
            return numFiles;
        }

        public int getNumLines() {
            return numLines;
        }

        public long getDuration() {
            return duration;
        }

        public void addNumFiles(int numFiles) {
            this.numFiles += numFiles;
        }

        public void addNumLines(int numLines) {
            this.numLines += numLines;
        }

        public void setBom(@Nonnull Bom bom) {
            this.bom = bom;
        }

        public void setStringBom(@Nonnull String stringBom) {
            this.stringBom = stringBom;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        @Nonnull
        public Optional<JsonNode> toJson() throws JsonProcessingException, GeneratorException {
            final ObjectMapper mapper = new ObjectMapper();
            if (bom != null) {
                final BomJsonGenerator bomGenerator =
                        BomGeneratorFactory.createJson(Version.VERSION_16, this.bom);
                return Optional.of(mapper.readTree(bomGenerator.toJsonString()));
            } else if (stringBom != null) {
                return Optional.of(mapper.readTree(stringBom));
            } else {
                return Optional.empty();
            }
        }

        public void add(@Nonnull ScanResult sr) {
            numFiles += sr.numFiles;
            numLines += sr.numLines;
            duration += sr.duration;
            mergeBOM(sr.bom);
            stringBom = sr.stringBom;
        }

        private void mergeBOM(@Nonnull Bom source) {
            if (this.bom == null) {
                this.bom = source;
                return;
            }
            this.bom.getComponents().addAll(source.getComponents());
            this.bom.getDependencies().addAll(source.getDependencies());
        }
    }
}
