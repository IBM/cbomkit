package com.ibm.usecases.scanning.services.scan;

import com.ibm.domain.scanning.Commit;
import com.ibm.domain.scanning.GitUrl;
import com.ibm.domain.scanning.Revision;
import com.ibm.infrastructure.errors.ClientDisconnected;
import com.ibm.usecases.scanning.services.indexing.ProjectModule;
import com.ibm.usecases.scanning.services.indexing.PythonIndexService;
import com.ibm.usecases.scanning.services.scan.python.PythonScannerService;
import com.ibm.utils.AssetableProgressDispatcher;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PythonScannerServiceTest {

    @Test
    void test() throws ClientDisconnected {
        final AssetableProgressDispatcher assetableProgressDispatcher =
                new AssetableProgressDispatcher();
        // indexing
        final File projectDirectory = new File("src/test/testdata/python/pyca");
        final PythonIndexService pythonIndexService =
                new PythonIndexService(assetableProgressDispatcher, projectDirectory);
        final List<ProjectModule> projectModules = pythonIndexService.index(null);
        assertThat(projectModules).hasSize(1);
        final ProjectModule projectModule = projectModules.getFirst();
        assertThat(projectModule.inputFileList()).hasSize(1);
        // scanning
        final PythonScannerService pythonScannerService = new PythonScannerService(
                assetableProgressDispatcher,
                projectDirectory
        );
        pythonScannerService.scan(
                new GitUrl("https://github.com/keycloak/keycloak"),
                new Revision("main"),
                new Commit("9c2825eb0e64aa7ea40b8dc3605d37046f6a24cb"),
                null,
                projectModules);
        // check
        assetableProgressDispatcher.hasNumberOfDetections(5);

        assertThat(
                assetableProgressDispatcher.hasDetectionWithNameAt(
                        "SHA256",
                        "src/test/testdata/python/pyca/generate_key.py",
                        4))
                .isTrue();

        assertThat(
                assetableProgressDispatcher.hasDetectionWithNameAt(
                        "AES128-CBC-PKCS7",
                        "src/test/testdata/python/pyca/generate_key.py",
                        4))
                .isTrue();

        assertThat(
                assetableProgressDispatcher.hasDetectionWithNameAt(
                        "HMAC-SHA256",
                        "src/test/testdata/python/pyca/generate_key.py",
                        4))
                .isTrue();

        assertThat(
                assetableProgressDispatcher.hasDetectionWithNameAt(
                        "Fernet",
                        "src/test/testdata/python/pyca/generate_key.py",
                        4))
                .isTrue();

        assertThat(
                assetableProgressDispatcher.hasDetectionWithNameAt(
                        "secret-key",
                        "src/test/testdata/python/pyca/generate_key.py",
                        4))
                .isTrue();
    }
}
