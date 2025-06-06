/*
 * CBOMkit
 * Copyright (C) 2024 PQCA
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
package com.ibm.usecases.database.commands;

import app.bootstrap.core.cqrs.ICommand;
import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.cqrs.ICommandHandler;
import com.ibm.infrastructure.database.readmodels.CBOMReadRepository;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Handles {@link DeleteCBOMCommand} commands by removing the referenced CBOM
 * from the repository.
 */
@Singleton
public class DeleteCBOMCommandHandler implements ICommandHandler {

    private final CBOMReadRepository readRepository;
    private final ICommandBus commandBus;

    @Inject
    public DeleteCBOMCommandHandler(CBOMReadRepository readRepository, ICommandBus commandBus) {
        this.readRepository = readRepository;
        this.commandBus = commandBus;
    }

    @PostConstruct
    void register() {
        commandBus.register(this);
    }

    @Override
    public void handle(ICommand command) throws Exception {
        if (!(command instanceof DeleteCBOMCommand deleteCommand)) {
            throw new IllegalArgumentException("Invalid command type");
        }

        final ArcContainer container = Arc.container();
        container.requestContext().activate();
        try {
            String projectIdentifier = deleteCommand.projectIdentifier();
            this.readRepository
                    .findBy(projectIdentifier)
                    .ifPresent(existing -> this.readRepository.delete(existing.getId()));
        } finally {
            container.requestContext().terminate();
        }
    }
}
