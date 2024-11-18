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

import app.bootstrap.core.ddd.IDomainEvent;
import app.bootstrap.core.ddd.IDomainEventBus;
import app.bootstrap.core.ddd.IDomainEventListener;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DomainEventBus implements IDomainEventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEventBus.class);

    private final List<IDomainEventListener> subscribers;

    public DomainEventBus() {
        this.subscribers = new ArrayList<>();
    }

    @Override
    public void subscribe(@Nonnull IDomainEventListener listener) {
        if (subscribers.contains(listener)) {
            return;
        }
        subscribers.add(listener);
    }

    @Override
    public void unsubscribe(@Nonnull IDomainEventListener listener) {
        subscribers.remove(listener);
    }

    @Override
    public void publish(@Nonnull final IDomainEvent event) {
        try (final ExecutorService executors = Executors.newCachedThreadPool()) {
            LOGGER.info("sending domainEvent {}", event);
            final List<IDomainEventListener> copy = new ArrayList<>(subscribers);
            for (final IDomainEventListener listener : copy) {
                executors.submit(
                        () -> {
                            try {
                                listener.handleEvent(event);
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
