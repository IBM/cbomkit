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
package com.ibm.usecases.scanning.services.indexing;

import com.ibm.infrastructure.progress.IProgressDispatcher;
import jakarta.annotation.Nonnull;
import java.io.File;
import java.util.List;
import javax.annotation.Nullable;

public final class PythonIndexService extends IndexingService {

    public PythonIndexService(
            @Nonnull IProgressDispatcher progressDispatcher, @Nonnull File baseDirectory) {
        super(progressDispatcher, baseDirectory, "python", ".py");
    }

    @Override
    boolean isModule(@Nonnull File directory) {
        for (String builFileName : List.of("pyproject.toml", "setup.cfg", "setup.py")) {
            File f = new File(directory, builFileName);
            if (f.exists() && f.isFile()) {
                return true;
            }
        }
        return false;
    }

    @Nullable @Override
    IBuildType getMainBuildTypeFromModuleDirectory(@Nonnull File directory) {
        if (!directory.isDirectory()) {
            return null;
        }
        // toml
        final File tomlFile = new File(directory, "pyproject.toml");
        if (tomlFile.exists() && tomlFile.isFile()) {
            return PythonBuildType.TOML;
        }
        // setup
        for (String setupFileName : List.of("setup.cfg", "setup.py")) {
            final File setupFile = new File(directory, setupFileName);
            if (setupFile.exists() && setupFile.isFile()) {
                return PythonBuildType.SETUP;
            }
        }
        return null;
    }

    @Override
    boolean excludeFromIndexing(@Nonnull File file) {
        return file.getPath().contains("tests/");
    }
}
