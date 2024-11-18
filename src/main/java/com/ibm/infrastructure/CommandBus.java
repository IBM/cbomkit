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
package com.ibm.infrastructure;

import app.bootstrap.core.cqrs.ICommand;
import app.bootstrap.core.cqrs.ICommandBus;
import app.bootstrap.core.cqrs.ICommandHandler;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CommandBus implements ICommandBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBus.class);

    private final List<ICommandHandler> commandHandlers;

    public CommandBus() {
        this.commandHandlers = new ArrayList<>();
    }

    @Override
    public void register(@Nonnull ICommandHandler commandHandler) {
        this.commandHandlers.add(commandHandler);
    }

    @Override
    public void remove(@Nonnull ICommandHandler commandHandler) {
        this.commandHandlers.remove(commandHandler);
    }

    @Override
    public void send(@Nonnull ICommand command) {
        try (final ExecutorService executors = Executors.newCachedThreadPool()) {
            LOGGER.info("sending command {}", command);
            final List<ICommandHandler> copy = new ArrayList<>(commandHandlers);
            for (final ICommandHandler iCommandHandler : copy) {
                executors.submit(
                        () -> {
                            try {
                                iCommandHandler.handle(command);
                            } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        });
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
