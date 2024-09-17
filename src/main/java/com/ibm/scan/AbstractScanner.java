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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.ibm.Utils;
import com.ibm.message.IMessageDispatcher;
import com.ibm.output.IOutputFileFactory;
import com.ibm.output.cyclondx.CBOMOutputFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.annotation.Nonnull;
import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.config.ConfigProvider;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

public abstract class AbstractScanner implements IScanner {
    protected IMessageDispatcher iMessageDispatcher;
    protected File baseDir;

    @Nonnull
    protected String getProjectIdentifierFromPath(@Nonnull File file) {
        String clonePath =
                ConfigProvider.getConfig()
                        .getOptionalValue("clone-dir", String.class)
                        .orElse(System.getProperty("user.home") + "/.cbomkit/repos");
        String path = file.getPath();
        path = path.substring(clonePath.length() + 1);
        // remove repo dir
        int slashIdx = path.indexOf('/');
        if (slashIdx < 0) {
            return "";
        }
        path = path.substring(slashIdx + 1);

        if (path.contains("/src")) {
            path = path.replace("/src", "");
        }
        return path;
    }

    @Nonnull
    protected TestInputFileBuilder createTestFileBuilder(
            @Nonnull File directory, @Nonnull File file) throws IOException {
        return new TestInputFileBuilder("", file.getPath())
                .setProjectBaseDir(directory.toPath())
                .setContents(Files.readString(file.toPath()))
                .setCharset(UTF_8)
                .setType(InputFile.Type.MAIN);
    }

    @Nonnull
    protected Bom getFinalBom() {
        final com.ibm.plugin.ScannerManager scannerMgr =
                new com.ibm.plugin.ScannerManager(IOutputFileFactory.DEFAULT);
        final CBOMOutputFile cbomFile = (CBOMOutputFile) scannerMgr.getOutputFile();
        Bom bom = cbomFile.getBom();
        scannerMgr.reset();
        bom.getComponents().forEach(component -> Utils.sanitizeOccurrence(this.baseDir, component));
        return bom;
    }
}
