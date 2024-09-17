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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.Utils;
import com.ibm.message.IMessageDispatcher;
import com.ibm.model.api.Message;
import com.ibm.output.cyclondx.CBOMOutputFile;
import com.ibm.resources.v1.ScannerResource;
import java.io.File;
import javax.annotation.Nonnull;
import org.cyclonedx.model.Component;
import org.jboss.logging.Logger;

public class DetectionHandler {

    private static final Logger LOG = Logger.getLogger(DetectionHandler.class);
    private final IMessageDispatcher iMessageDispatcher;
    private final File clonedProject;
    private final ObjectMapper mapper = new ObjectMapper();

    public DetectionHandler(
            @Nonnull IMessageDispatcher iMessageDispatcher, @Nonnull File clonedProject) {
        this.clonedProject = clonedProject;
        this.iMessageDispatcher = iMessageDispatcher;
    }

    public void handleDetection(@Nonnull CBOMOutputFile componentCBOM) {
        for (Component c : componentCBOM.getBom().getComponents()) {
            String cname = c.getName();
            LOG.info("Detected finding: " + cname);
            Utils.sanitizeOccurrence(clonedProject, c);
            this.sendDetection(c);
        }
    }

    private void sendDetection(@Nonnull Component component) {
        try {
            String componentJsonSTring = mapper.writeValueAsString(component);
            iMessageDispatcher.sendMessage(Message.Type.DETECTION, componentJsonSTring);
        } catch (JsonProcessingException | ScannerResource.CancelScanException e) {
            // catch various errors
            LOG.error(e.getLocalizedMessage());
        }
    }
}
