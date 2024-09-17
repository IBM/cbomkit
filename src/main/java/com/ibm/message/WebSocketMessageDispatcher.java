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
package com.ibm.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.model.api.Message;
import com.ibm.resources.v1.ScannerResource;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jboss.logging.Logger;

public class WebSocketMessageDispatcher implements IMessageDispatcher {
    private static final Logger LOG = Logger.getLogger(WebSocketMessageDispatcher.class);

    @Nonnull private final Session session;

    public WebSocketMessageDispatcher(@Nonnull Session session) {
        this.session = session;
    }

    @Override
    public void sendLabelMessage(@Nonnull String message)
            throws ScannerResource.CancelScanException {
        sendMessage(session, Message.Type.LABEL, message, null);
    }

    @Override
    public void sendCBOMMessage(@Nonnull String message)
            throws ScannerResource.CancelScanException {
        sendMessage(session, Message.Type.CBOM, message, null);
    }

    @Override
    public void sendPurlMessage(@Nonnull List<String> purls)
            throws ScannerResource.CancelScanException {
        sendMessage(session, Message.Type.PURL, null, purls);
    }

    @Override
    public void sendErrorMessage(@Nonnull String error) throws ScannerResource.CancelScanException {
        sendMessage(session, Message.Type.ERROR, error, null);
    }

    @Override
    public void sendMessage(@Nonnull Message.Type type, @Nonnull String str)
            throws ScannerResource.CancelScanException {
        sendMessage(session, type, str, null);
    }

    private void sendMessage(
            @Nonnull Session session,
            @Nonnull Message.Type type,
            @Nullable String str,
            @Nullable List<String> purls)
            throws ScannerResource.CancelScanException {
        ObjectMapper mapper = new ObjectMapper();
        final CountDownLatch errorLatch = new CountDownLatch(1);
        try {
            // convert a user object to JSON string and return it
            Message m = new Message();
            m.setType(type);
            m.setMessage(str);
            m.setPurls(purls);
            String jsonMessage = mapper.writeValueAsString(m);

            final AtomicBoolean errOccurred = new AtomicBoolean(false);
            session.getAsyncRemote()
                    .sendObject(
                            jsonMessage,
                            sendResult -> {
                                if (sendResult.getException() != null) {
                                    LOG.error(sendResult.getException());
                                    errOccurred.set(true);
                                    errorLatch.countDown();
                                    try {
                                        session.close();
                                    } catch (IOException e) {
                                        LOG.error(e.getLocalizedMessage());
                                    }
                                } else {
                                    errorLatch.countDown();
                                }
                            });
            errorLatch.await();
            if (errOccurred.get()) {
                throw new ScannerResource.CancelScanException(
                        "Client disconnected " + session.getId());
            }
        } catch (JsonProcessingException e) {
            LOG.error(e.getLocalizedMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScannerResource.RuntimeScanException(e);
        }
    }
}
