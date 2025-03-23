/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;

/**
 * Limit the amount of data read to prevent a {@link OutOfMemoryError}.
 *
 * @author Alexander Schwartz
 */
class SafeBasicResponseHandler extends BasicResponseHandler {
    private final long maxConsumedResponseSize;

    SafeBasicResponseHandler(long maxConsumedResponseSize) {
        this.maxConsumedResponseSize = maxConsumedResponseSize;
    }

    @Override
    public String handleEntity(HttpEntity entity) throws IOException {
        return super.handleEntity(new SafeHttpEntity(entity, maxConsumedResponseSize));
    }
}
