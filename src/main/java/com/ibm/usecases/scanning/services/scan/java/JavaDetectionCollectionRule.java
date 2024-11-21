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
package com.ibm.usecases.scanning.services.scan.java;

import com.ibm.engine.detection.Finding;
import com.ibm.mapper.model.INode;
import com.ibm.plugin.rules.JavaInventoryRule;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaDetectionCollectionRule extends JavaInventoryRule {
    private final Consumer<List<INode>> handler;

    public JavaDetectionCollectionRule(@Nonnull Consumer<List<INode>> findingConsumer) {
        this.handler = findingConsumer;
    }

    @Override
    public void update(@Nonnull Finding<JavaCheck, Tree, Symbol, JavaFileScannerContext> finding) {
        super.update(finding);
        final List<INode> nodes = javaTranslationProcess.initiate(finding.detectionStore());
        handler.accept(nodes);
    }
}
