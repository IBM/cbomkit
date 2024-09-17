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

import com.ibm.message.IMessageDispatcher;
import com.ibm.model.api.Message;
import com.ibm.model.api.ScanRequest;
import com.ibm.resources.v1.ScannerResource.CancelScanException;
import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.Unmodifiable;

public record ScannerManager(@Nonnull List<IScanner> scanners) implements IScannerManager {

    @Override
    @Nonnull
    @Unmodifiable
    public List<IScanner> scanners() {
        return Collections.unmodifiableList(scanners);
    }

    @Nonnull
    public IScanner.ScanResult scan(
            @Nonnull IMessageDispatcher iMessageDispatcher,
            @Nonnull File clonedProject,
            @Nonnull ScanRequest request)
            throws CancelScanException {
        final IScanner.ScanResult result = new IScanner.ScanResult();
        for (IScanner scanner : this.scanners) {
            scanner.init(iMessageDispatcher, clonedProject, request);
            // merge results of all used scanners
            result.add(scanner.scan());
        }

        iMessageDispatcher.sendMessage(
                Message.Type.SCANNED_DURATION, String.valueOf(result.getDuration()));
        iMessageDispatcher.sendMessage(
                Message.Type.SCANNED_FILE_COUNT, String.valueOf(result.getNumFiles()));
        iMessageDispatcher.sendMessage(
                Message.Type.SCANNED_NUMBER_OF_LINES, String.valueOf(result.getNumLines()));
        return result;
    }
}
