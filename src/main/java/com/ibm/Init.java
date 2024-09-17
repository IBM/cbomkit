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
package com.ibm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.ibm.model.Identifiers;
import com.ibm.model.PurlVersion;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.util.*;
import javax.annotation.Nonnull;
import org.jboss.logging.Logger;

public class Init implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(Init.class);

    @Override
    public int run(String... args) throws Exception {
        try (InputStream in =
                // Thread.currentThread().getContextClassLoader().getResourceAsStream("purls.json"))
                // {
                this.getClass().getClassLoader().getResourceAsStream("purls.json")) {
            LOG.info("Try to load purls");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readValue(in, JsonNode.class);
            initPurls(jsonNode);
            LOG.info("Purls are up to date!");
        }
        Quarkus.waitForExit();
        return 0;
    }

    /**
     * This method checks the purl data in the database and updates them if necessary. This method
     * is decorated with the <code>@Transactional</code> annotation, which ensures that all
     * operations within the method are executed as a single database transaction.
     */
    @Transactional
    public void initPurls(JsonNode jsonNode) {
        final String version = jsonNode.get("version").textValue();
        final Optional<PurlVersion> purlVersion = getPurlVersion();
        if (shouldUpdatePurlData(purlVersion, version)) {
            LOG.info("Found new version of purl file (" + version + ")");
            LOG.info("Loading purls from purl file...");
            storePurlData(jsonNode);
            storeNewPurlDataVersion(version);
        }
    }

    /**
     * This method stores the PackageURL data from a JSON file and persists it to the database. It
     * takes a single parameter, <code>jsonNode</code>, which represents the JSON data to be stored.
     * <br>
     * If a Package URL is found, the method checks if it's for Git by calling the <code>purlForGit
     * </code> method. If the URL is for Git, the method extracts the identifier from the URL and
     * adds it to a list of identifiers. The method then creates or updates an <code>Identifiers
     * </code> object in the database with the identifier and PackageURL data for each field. <br>
     * Once all fields have been processed, the method persists all of the <code>Identifiers</code>
     * objects to the database.
     */
    private void storePurlData(@Nonnull JsonNode jsonNode) {
        Iterator<String> fields = jsonNode.fieldNames();
        HashMap<String, Identifiers> appIds = new HashMap<>();
        int nFields = 0;
        while (fields.hasNext()) {
            final String field = fields.next();
            Iterator<JsonNode> elements = jsonNode.get(field).elements();
            List<String> identiferPurls = new ArrayList<>();
            List<String> purls = new ArrayList<>();
            while (elements.hasNext()) {
                final String purl = elements.next().textValue();
                purls.add(purl);
                if (purlForGit(purl)) {
                    extractIdentifierFromPurl(purl).ifPresent(identiferPurls::add);
                }
            }
            for (String id : identiferPurls) {
                Identifiers appId = appIds.get(id);
                if (appId == null) {
                    appId = new Identifiers(id, purls);
                    appIds.put(id, appId);
                } else {
                    appId.setPurls(purls);
                }
            }
            nFields++;
            if (nFields % 1000 == 0) {
                LOG.info(nFields + " fields processed");
            }
        }
        LOG.info(nFields + " fields processed");
        Identifiers.persist(appIds.values());
        LOG.info("Purls persisted");
    }

    /**
     * This method checks if a given string contains any of the following git-related keywords:
     * "github", "gitlab", or "bitbucket".
     */
    private boolean purlForGit(String str) {
        if (str == null) {
            return false;
        }
        return str.contains("github") || str.contains("gitlab") || str.contains("bitbucket");
    }

    /**
     * Method isGolangGitPurl takes a String parameter <code>str</code> and returns a boolean value
     * indicating whether the given string contains any of the specified domain names or not.
     *
     * @param str The input string to check for the presence of the domain names.
     * @return true if the string contains any of the specified domain names, false otherwise.
     */
    private boolean isGolangGitPurl(String str) {
        if (str == null) {
            return false;
        }
        return str.contains("github.com")
                || str.contains("gitlab.com")
                || str.contains("bitbucket.org");
    }

    /**
     * Extracts an identifier from a Package URL (PURL) string.
     *
     * <p>The PURL is expected to be in the form of either a 'normal' or Golang-related PURL. For a
     * normal PURL, the method will extract the class name as the identifier. For a Go PURL, the
     * method will extract the namespace and module name as the identifiers. If the PURL is not a
     * valid Java or Go PURL, the method will return an empty Optional.<br>
     *
     * @param purl The Package URL (PURL) string to extract an identifier from
     * @return An Optional containing the extracted identifier, or an empty Optional if the PURL is
     *     invalid
     */
    private Optional<String> extractIdentifierFromPurl(String purl) {
        try {
            PackageURL packageURL = new PackageURL(purl);
            final String type = packageURL.getType();
            final String namespace = packageURL.getNamespace();
            StringBuilder builder;
            if (purlForGit(type)) {
                builder = new StringBuilder();
                builder.append(type).append("/");
                if (packageURL.getNamespace() != null) {
                    builder.append(packageURL.getNamespace()).append("/");
                }
                if (packageURL.getName() != null) {
                    builder.append(packageURL.getName());
                }
                return Optional.of(builder.toString());
            } else if (isGolangGitPurl(namespace)) {
                builder = new StringBuilder();
                builder.append(namespace).append("/");
                if (packageURL.getName() != null) {
                    builder.append(packageURL.getName());
                }
                return Optional.of(builder.toString());
            }
            return Optional.empty();
        } catch (MalformedPackageURLException e) {
            return Optional.empty();
        }
    }

    /** This method reads the PackageURL version data from the database. */
    public Optional<PurlVersion> getPurlVersion() {
        return PurlVersion.find("id", 1).singleResultOptional();
    }

    /**
     * This method checks if the peristed purl data should be updated by comparing PurlVersion with
     * the given file version. It returns true if the PurlVersion optional is either empty or its
     * version tag differs from the given file version.
     */
    private boolean shouldUpdatePurlData(Optional<PurlVersion> purlVersion, String fileVersion) {
        if (purlVersion.isPresent()) {
            return !Objects.equals(fileVersion.trim(), purlVersion.get().getVersion());
        }
        return true;
    }

    /**
     * This method perists the new PackageURL version data to the database. It takes a single
     * parameter, <code>fileVersion</code>, which represents the new purl version as read from the
     * json file.
     */
    private void storeNewPurlDataVersion(@Nonnull String fileVersion) {
        LOG.info("Persisting purl version");
        // This will overwrite any existing purl version record
        (new PurlVersion(fileVersion)).persist();
        LOG.info("Purl version persisted");
    }
}
