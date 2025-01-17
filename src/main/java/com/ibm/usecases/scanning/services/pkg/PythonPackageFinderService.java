/*
 * CBOMkit
 * Copyright (C) 2025 IBM
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
package com.ibm.usecases.scanning.services.pkg;

import jakarta.annotation.Nonnull;
import java.nio.file.Path;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public class PythonPackageFinderService extends PackageFinderService {

    public PythonPackageFinderService(@Nonnull Path root) throws IllegalArgumentException {
        super(root);
    }

    @Override
    public boolean isBuildFile(Path file) {
        return file.endsWith("pyproject.toml");
    }

    @Override
    public String getPackageName(Path buildFile) throws Exception {
        TomlParseResult result = Toml.parse(buildFile);
        return result.getString(("project.name"));
    }
}
