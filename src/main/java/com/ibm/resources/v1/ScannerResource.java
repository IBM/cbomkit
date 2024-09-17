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
package com.ibm.resources.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.Utils;
import com.ibm.configuration.IConfiguration;
import com.ibm.git.GitService;
import com.ibm.message.IMessageDispatcher;
import com.ibm.message.WebSocketMessageDispatcher;
import com.ibm.model.IdentifiableScan;
import com.ibm.model.Identifiers;
import com.ibm.model.IdentifiersInternal;
import com.ibm.model.Scan;
import com.ibm.model.api.Message;
import com.ibm.model.api.ScanRequest;
import com.ibm.scan.IScanner;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.apache.commons.io.FileUtils;
import org.cyclonedx.Version;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

@SuppressWarnings("java:S3252")
@ServerEndpoint("/v1/scan/{clientId}")
@ApplicationScoped
public class ScannerResource {
    private static final Logger LOG = Logger.getLogger(ScannerResource.class);

    record GitRepo(File cloneDir, String commitHash) {}

    @Nonnull final Map<String, Session> sessions;
    @Nonnull final IConfiguration configuration;

    @Inject
    public ScannerResource(@Nonnull final IConfiguration configuration) {
        this.configuration = configuration;
        this.sessions = new ConcurrentHashMap<>();
    }

    static Optional<GitRepo> cloneGitRepo(
            @Nonnull ScanRequest request, @Nonnull IMessageDispatcher iMessageDispatcher) {
        if (request.branch() == null) {
            return Optional.empty();
        }

        File cloneDir = null;
        try {
            GitService gitService =
                    new GitService(Objects.requireNonNull(request.gitUrl()), request.branch());
            cloneDir =
                    gitService.createCloneDirectory(
                            Objects.requireNonNull(
                                    ConfigProvider.getConfig()
                                            .getOptionalValue("clone-dir", String.class)
                                            .orElse(
                                                    System.getProperty("user.home")
                                                            + "/.cbomkit/repos")),
                            () -> request.gitUrl().concat(request.branch()));

            iMessageDispatcher.sendLabelMessage("Cloning git repository");
            LOG.info("Try to clone git repo: " + request.gitUrl());

            String commitHash =
                    gitService.cloneRepository(
                            cloneDir,
                            message -> {
                                try {
                                    iMessageDispatcher.sendLabelMessage(message);
                                } catch (CancelScanException e) {
                                    // nothing
                                }
                            });
            iMessageDispatcher.sendMessage(Message.Type.REVISION_HASH, commitHash);

            LOG.info("Completed Cloning");
            iMessageDispatcher.sendLabelMessage("Completed Cloning");

            return Optional.of(new GitRepo(cloneDir, commitHash));
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
            try {
                FileUtils.deleteDirectory(Objects.requireNonNull(cloneDir));
            } catch (Exception ignored) {
                /*ignored*/
            }
            return Optional.empty();
        }
    }

    private static @Nonnull String getSearchPurl(@Nonnull String gitUrl) throws URISyntaxException {
        final URI uri = new URI(gitUrl);
        final String host = uri.getHost();
        String domainName = host.startsWith("www.") ? host.substring(4) : host;
        if (domainName.endsWith(".com") || domainName.endsWith(".org")) {
            domainName = domainName.substring(0, domainName.length() - 4);
        }
        String path = uri.getPath();
        if (path.contains(".git")) {
            path = path.substring(0, path.indexOf(".git"));
        }
        return domainName + path;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("clientId") String clientId) {
        LOG.info("Session open for id " + clientId);
        sessions.put(clientId, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("clientId") String clientId) {
        LOG.warn("asking to close:" + clientId);
        sessions.remove(clientId);
    }

    @OnError
    public void onError(
            Session session, @PathParam("clientId") String clientId, Throwable throwable) {
        sessions.remove(clientId);
    }

    @OnMessage
    public void onMessage(String requestJSON, @PathParam("clientId") String clientId) {
        LOG.info("Got request: " + requestJSON);
        final Optional<Session> possibleSession = Optional.ofNullable(sessions.get(clientId));
        Optional<GitRepo> clonedProject = Optional.empty();
        Optional<JsonNode> cbom = Optional.empty();
        try {
            final Session session =
                    possibleSession.orElseThrow(
                            () -> new CancelScanException("No valid session established"));
            final WebSocketMessageDispatcher webSocketMessageDispatcher =
                    new WebSocketMessageDispatcher(session);
            webSocketMessageDispatcher.sendLabelMessage("Starting...");

            final ScanRequest scanRequest =
                    new ObjectMapper().readValue(requestJSON, ScanRequest.class);
            final Optional<IdentifiersInternal> possibleIdentifiers =
                    getIdentifier(scanRequest.gitUrl());
            if (possibleIdentifiers.isPresent()) {
                webSocketMessageDispatcher.sendPurlMessage(possibleIdentifiers.get().getPurls());
            }

            clonedProject = cloneGitBranch(scanRequest, webSocketMessageDispatcher);
            if (clonedProject.isPresent()) {
                cbom = runScan(webSocketMessageDispatcher, clonedProject.get(), scanRequest);
                // try to store
                if (possibleIdentifiers.isPresent() && cbom.isPresent()) {
                    List<IdentifiableScan> identifiableScans =
                            storeCBOM(
                                    cbom.get(),
                                    possibleIdentifiers.get(),
                                    scanRequest.gitUrl(),
                                    scanRequest.branch());
                    Utils.addProperties(
                            cbom.get(),
                            scanRequest,
                            clonedProject.get().commitHash,
                            identifiableScans);
                    LOG.info("Scan related data persisted");
                    webSocketMessageDispatcher.sendCBOMMessage(cbom.get().toString());
                }
            }
            webSocketMessageDispatcher.sendLabelMessage("Finished");
        } catch (Exception e) {
            possibleSession.ifPresent(
                    session -> handleError(new WebSocketMessageDispatcher(session), e));
        } finally {
            if (cbom.isEmpty()) {
                possibleSession.ifPresent(
                        session ->
                                handleError(
                                        new WebSocketMessageDispatcher(session),
                                        new Exception("Scan failed - no CBOM")));
            }
            if (clonedProject.isPresent()) {
                try {
                    FileUtils.deleteDirectory(clonedProject.get().cloneDir);
                    LOG.info("Deleted cloned repo");
                } catch (IOException ignored) {
                    LOG.info("Failed to delete cloned repo");
                }
            }
        }
    }

    private void handleError(@Nonnull IMessageDispatcher iMessageDispatcher, @Nonnull Exception e) {
        try {
            LOG.error(e.getLocalizedMessage());
            iMessageDispatcher.sendErrorMessage(e.getLocalizedMessage());
        } catch (Exception exception) {
            LOG.error(exception.getLocalizedMessage());
        }
    }

    private Optional<GitRepo> cloneGitBranch(
            @Nonnull ScanRequest scanRequest, @Nonnull IMessageDispatcher iMessageDispatcher)
            throws CancelScanException {
        Optional<GitRepo> clonedProject;
        if (scanRequest.branch() != null) {
            clonedProject = cloneGitRepo(scanRequest, iMessageDispatcher);
        } else {
            scanRequest.setBranch("main");
            clonedProject = cloneGitRepo(scanRequest, iMessageDispatcher);
            if (clonedProject.isEmpty()) {
                scanRequest.setBranch("master");
                clonedProject = cloneGitRepo(scanRequest, iMessageDispatcher);
                if (clonedProject.isEmpty()) {
                    iMessageDispatcher.sendErrorMessage(
                            "Could no clone git repo branch 'main' or 'master'. Try to specify the branch.");
                    scanRequest.setBranch(null);
                } else {
                    iMessageDispatcher.sendMessage(Message.Type.BRANCH, "master");
                }
            } else {
                iMessageDispatcher.sendMessage(Message.Type.BRANCH, "main");
            }
        }
        return clonedProject;
    }

    private Optional<JsonNode> runScan(
            @Nonnull IMessageDispatcher iMessageDispatcher,
            @Nonnull GitRepo clonedProject,
            @Nonnull ScanRequest scanRequest)
            throws CancelScanException {
        final IScanner.ScanResult scanResult =
                configuration
                        .getScannerManager()
                        .scan(iMessageDispatcher, clonedProject.cloneDir, scanRequest);
        try {
            return scanResult.toJson();
        } catch (JsonProcessingException jpe) {
            LOG.error("Cannot parse CBOM", jpe);
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<IdentifiersInternal> getIdentifier(@Nonnull String gitUrl) {
        try {
            final String searchPurl = getSearchPurl(gitUrl);
            Identifiers identifiers = Identifiers.find("gitIdentifier", searchPurl).firstResult();
            if (identifiers == null) {
                String purl = "pkg:" + searchPurl;
                identifiers = new Identifiers(searchPurl, List.of(purl));
                Identifiers.persist(identifiers);
            }
            final IdentifiersInternal internal =
                    new IdentifiersInternal(identifiers.getGitIdentifier(), identifiers.getPurls());
            return Optional.of(internal);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public List<IdentifiableScan> storeCBOM(
            @Nonnull JsonNode cbom,
            @Nonnull IdentifiersInternal identifiers,
            @Nonnull String gitUrl,
            @Nonnull String branch) {
        final PanacheQuery<Scan> findCbomForGitAndBranch =
                Scan.find("gitUrl = ?1 and branch = ?2", gitUrl, branch);
        final Optional<Scan> possibleCbom = findCbomForGitAndBranch.firstResultOptional();

        Scan entity = new Scan();
        if (possibleCbom.isPresent()) {
            LOG.info(
                    "CBOM-Entity with giturl "
                            + possibleCbom.get().getGitUrl()
                            + " already present. Will be overwritten.");
            entity = possibleCbom.get();
        }
        entity.setBom(cbom);
        entity.setGitUrl(gitUrl);
        entity.setBranch(branch);
        entity.setCbomSpecVersion(Version.VERSION_16.getVersionString());
        entity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        entity.persist();
        if (entity.isPersistent()) {
            LOG.info("CBOM-Entity object with gitUrl " + entity.getGitUrl() + " persisted");
        } else {
            LOG.error("An error occurred while persisting the CBOM-Entity.");
        }

        final Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+\\.?\\d*)");
        final Matcher versionMatcher = versionPattern.matcher(branch);
        String version = null;
        if (versionMatcher.find()) {
            version = versionMatcher.group(1);
        }

        List<IdentifiableScan> identifiableScans = new ArrayList<>();
        for (String purl : identifiers.getPurls()) {
            String finalPurlString = purl;
            if (version != null) {
                finalPurlString = purl + "@" + version;
            }
            Optional<IdentifiableScan> possiblePurl =
                    IdentifiableScan.find("purl", finalPurlString).firstResultOptional();
            IdentifiableScan identifiableScan1 = new IdentifiableScan();
            if (possiblePurl.isPresent()) {
                identifiableScan1 = possiblePurl.get();
            }
            if (!identifiableScan1.getScans().contains(entity)) {
                identifiableScan1.addScan(entity);
            }
            identifiableScan1.setPurl(finalPurlString);
            identifiableScans.add(identifiableScan1);
        }
        IdentifiableScan.persist(identifiableScans);

        return identifiableScans;
    }

    public static class CancelScanException extends Exception {
        public CancelScanException(String message) {
            super(message);
        }
    }

    public static class RuntimeScanException extends RuntimeException {
        public RuntimeScanException(Exception e) {
            super(e);
        }
    }
}
