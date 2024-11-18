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
package com.ibm.usecases.scanning.services.git;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;
import org.eclipse.jgit.lib.ProgressMonitor;

public final class GitProgressMonitor implements ProgressMonitor {
    @Nonnull private final Consumer<String> messageHandling;
    private int currentTaskTotalWork;
    private String currentTaskTitle;
    private int currentTaskWork = 0;

    private static final String PREFIX_CLONING_GIT_REPOSITORY = "Cloning git repository: ";

    public GitProgressMonitor(@Nonnull Consumer<String> messageHandling) {
        this.messageHandling = messageHandling;
    }

    @Override
    public void start(int i) {
        // do nothing
    }

    @Override
    public void beginTask(String s, int i) {
        this.currentTaskTotalWork = i;
        this.currentTaskTitle = s;
        this.currentTaskWork = 0;
        this.messageHandling.accept(PREFIX_CLONING_GIT_REPOSITORY + s + " " + 0 + "%");
    }

    @Override
    public void update(int i) {
        int percentage;
        if (i == 1) {
            this.currentTaskWork++;
            percentage = calculatePercentage(this.currentTaskWork, this.currentTaskTotalWork);
            if (percentage % 5 == 0) {
                this.messageHandling.accept(
                        PREFIX_CLONING_GIT_REPOSITORY
                                + this.currentTaskTitle
                                + " "
                                + percentage
                                + "%");
            }
        }
    }

    @Override
    public void endTask() {
        this.messageHandling.accept(
                PREFIX_CLONING_GIT_REPOSITORY + this.currentTaskTitle + " done");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void showDuration(boolean b) {
        // nothing
    }

    private static int calculatePercentage(int numerator, int denominator) {
        double percentage = (double) numerator / denominator * 100;
        return (int) Math.round(percentage);
    }
}
