package com.ibm.usecases.scanning.services.indexing;

import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.infrastructure.progress.IProgressDispatcher;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PythonIndexServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PythonIndexServiceTest.class);

    @Test
    void test() throws ClientDisconnected {
        final IProgressDispatcher progressDispatcher =
                progressMessage -> LOGGER.info(progressMessage.toString());

        final PythonIndexService pythonIndexService =
                new PythonIndexService(progressDispatcher, new File("src/test/testdata/python/pyca"));
        final List<ProjectModule> projectModules = pythonIndexService.index(null);
        assertThat(projectModules).hasSize(1);
        final ProjectModule projectModule = projectModules.getFirst();
        assertThat(projectModule.inputFileList()).hasSize(1);
    }
}
