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

import com.ibm.engine.detection.Finding;
import com.ibm.mapper.model.INode;
import com.ibm.message.IMessageDispatcher;
import com.ibm.output.cyclondx.CBOMOutputFile;
import com.ibm.output.cyclondx.CBOMOutputFileFactory;
import com.ibm.plugin.rules.PythonInventoryRule;
import java.io.File;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.plugins.python.api.PythonCheck;
import org.sonar.plugins.python.api.PythonVisitorContext;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.tree.Tree;

public class PythonCryptoDetection extends PythonInventoryRule {
    private final DetectionHandler handler;
    private final CBOMOutputFileFactory cbomFactory = new CBOMOutputFileFactory();

    public PythonCryptoDetection(IMessageDispatcher iMessageDispatcher, File clonedProject) {
        this.handler = new DetectionHandler(iMessageDispatcher, clonedProject);
    }

    @Override
    public void update(@Nonnull Finding<PythonCheck, Tree, Symbol, PythonVisitorContext> finding) {
        super.update(finding);
        final List<INode> nodes = pythonTranslationProcess.initiate(finding.detectionStore());
        final CBOMOutputFile componentCBOM = cbomFactory.createOutputFormat(nodes);
        handler.handleDetection(componentCBOM);
    }
}
