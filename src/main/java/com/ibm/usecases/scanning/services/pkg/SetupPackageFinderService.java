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
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ini4j.Ini;

public class SetupPackageFinderService extends PackageFinderService {

    public SetupPackageFinderService(@Nonnull Path root) throws IllegalArgumentException {
        super(root);
    }

    @Override
    public boolean isBuildFile(Path file) {
        return file.endsWith("setup.cfg") || file.endsWith("setup.py");
    }

    @Override
    public String getPackageName(Path buildFile) throws Exception {
        if (buildFile.endsWith("cfg")) {
            Ini cfg = new Ini(buildFile.toFile());
            return cfg.get("metadata", "name");
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(buildFile.toFile()))) {
                String line;
                Pattern pattern = Pattern.compile("name\\s*=\\s*['\"]([^'\"]*)['\"]");

                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
            return null;
        }
    }
}
