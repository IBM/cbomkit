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
package com.ibm.usecases.scanning.eventhandler;

import app.bootstrap.core.cqrs.DomainEventHandler;
import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.ddd.IDomainEvent;
import app.bootstrap.core.ddd.IDomainEventBus;
import com.ibm.domain.scanning.ScanId;
import com.ibm.domain.scanning.events.PurlScanRequestedEvent;
import com.ibm.domain.scanning.events.ScanRequestedEvent;
import com.ibm.usecases.scanning.commands.CloneGitRepositoryCommand;
import com.ibm.usecases.scanning.commands.FetchDataFromDepsDevCommand;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

@Singleton
public final class ScanEventHandler extends DomainEventHandler {

    void onStart(@Observes StartupEvent event) {
        this.domainEventBus.subscribe(this);
    }

    public ScanEventHandler(
            @Nonnull ICommandBus commandBus, @Nonnull IDomainEventBus domainEventBus) {
        super(commandBus, domainEventBus);
    }

    @Override
    public void handleEvent(@Nonnull IDomainEvent event) throws Exception {
        switch (event) {
            case ScanRequestedEvent scanRequestedEvent ->
                    this.handleScanRequested(scanRequestedEvent);
            case PurlScanRequestedEvent purlScanRequestedEvent ->
                    this.handlePurlScanRequested(purlScanRequestedEvent);
            default -> {
                // nothing
            }
        }
    }

    private void handleScanRequested(@Nonnull ScanRequestedEvent scanRequestedEvent) {
        final ScanId scanId = scanRequestedEvent.getScanId();
        this.commandBus.send(
                new CloneGitRepositoryCommand(scanId, scanRequestedEvent.getCredentials()));
    }

    private void handlePurlScanRequested(@Nonnull PurlScanRequestedEvent purlScanRequestedEvent) {
        final ScanId scanId = purlScanRequestedEvent.getScanId();
        this.commandBus.send(
                new FetchDataFromDepsDevCommand(scanId, purlScanRequestedEvent.getCredentials()));
    }
}
