/*
 * CBOMkit
 * Copyright (C) 2025 IBM
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
package com.ibm.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import com.ibm.infrastructure.progress.ProgressMessage;
import com.ibm.infrastructure.progress.ProgressMessageType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetableProgressDispatcher implements IProgressDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssetableProgressDispatcher.class);

    private final List<Component> components = new ArrayList<>();

    @Override
    public void send(@Nonnull ProgressMessage progressMessage) throws ClientDisconnected {
        try {
            LOGGER.info(progressMessage.toString());
            if (progressMessage.type() == ProgressMessageType.DETECTION) {
                final ObjectMapper mapper = new ObjectMapper();
                final Component component =
                        mapper.readValue(progressMessage.message(), Component.class);
                this.components.add(component);
            }
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public boolean hasDetectionAt(@Nullable String location, @Nullable Integer line) {
        if (location == null && line == null) {
            return false;
        }
        for (Component component : this.components) {
            final List<Occurrence> occurrences = component.getEvidence().getOccurrences();
            assertThat(occurrences).isNotEmpty();
            boolean locationResult = false;
            boolean lineResult = false;
            if (location != null) {
                locationResult =
                        occurrences.stream()
                                .map(Occurrence::getLocation)
                                .anyMatch(location::equals);
            }
            if (line != null) {
                lineResult = occurrences.stream().map(Occurrence::getLine).anyMatch(line::equals);
            }
            if (locationResult && lineResult) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDetectionWithNameAt(
            @Nonnull String name, @Nonnull String location, @Nonnull Integer line) {
        for (Component component : this.components) {
            boolean nameResult =
                    component.getName().equals(name) || component.getName().contains(name);

            final List<Occurrence> occurrences = component.getEvidence().getOccurrences();
            assertThat(occurrences).isNotEmpty();

            boolean locationResult =
                    occurrences.stream().map(Occurrence::getLocation).anyMatch(location::equals);
            boolean lineResult =
                    occurrences.stream().map(Occurrence::getLine).anyMatch(line::equals);

            if (nameResult && locationResult && lineResult) {
                return true;
            }
        }
        return false;
    }

    public void hasNumberOfDetections(int number) {
        assertThat(components).hasSize(number);
    }
}
