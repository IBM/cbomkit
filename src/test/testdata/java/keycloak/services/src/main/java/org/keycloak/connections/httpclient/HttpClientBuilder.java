/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.httpclient;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.keycloak.common.enums.HostnameVerificationPolicy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction for creating HttpClients. Allows SSL configuration.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HttpClientBuilder {

    /**
     * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
     * @version $Revision: 1 $
     */
    private static class PassthroughTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    protected KeyStore truststore;
    protected KeyStore clientKeyStore;
    protected String clientPrivateKeyPassword;
    protected boolean disableTrustManager;
    protected HostnameVerificationPolicy policy = HostnameVerificationPolicy.DEFAULT;
    protected SSLContext sslContext;
    protected int connectionPoolSize = 128;
    protected int maxPooledPerRoute = 64;
    protected long connectionTTL = -1;
    protected boolean reuseConnections = true;
    protected TimeUnit connectionTTLUnit = TimeUnit.MILLISECONDS;
    protected long maxConnectionIdleTime = 900000;
    protected TimeUnit maxConnectionIdleTimeUnit = TimeUnit.MILLISECONDS;
    protected long socketTimeout = -1;
    protected TimeUnit socketTimeoutUnits = TimeUnit.MILLISECONDS;
    protected long establishConnectionTimeout = -1;
    protected TimeUnit establishConnectionTimeoutUnits = TimeUnit.MILLISECONDS;
    protected boolean disableCookies = false;
    protected ProxyMappings proxyMappings;
    protected boolean expectContinueEnabled = false;

    /**
     * Socket inactivity timeout
     *
     * @param timeout
     * @param unit
     * @return
     */
    public HttpClientBuilder socketTimeout(long timeout, TimeUnit unit)
    {
        this.socketTimeout = timeout;
        this.socketTimeoutUnits = unit;
        return this;
    }

    /**
     * When trying to make an initial socket connection, what is the timeout?
     *
     * @param timeout
     * @param unit
     * @return
     */
    public HttpClientBuilder establishConnectionTimeout(long timeout, TimeUnit unit)
    {
        this.establishConnectionTimeout = timeout;
        this.establishConnectionTimeoutUnits = unit;
        return this;
    }

    public HttpClientBuilder connectionTTL(long ttl, TimeUnit unit) {
        this.connectionTTL = ttl;
        this.connectionTTLUnit = unit;
        return this;
    }

    public HttpClientBuilder reuseConnections(boolean reuseConnections) {
        this.reuseConnections = reuseConnections;
        return this;
    }

    public HttpClientBuilder maxConnectionIdleTime(long maxConnectionIdleTime, TimeUnit unit) {
        this.maxConnectionIdleTime = maxConnectionIdleTime;
        this.maxConnectionIdleTimeUnit = unit;
        return this;
    }

    public HttpClientBuilder maxPooledPerRoute(int maxPooledPerRoute) {
        this.maxPooledPerRoute = maxPooledPerRoute;
        return this;
    }

    public HttpClientBuilder connectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
        return this;
    }

    /**
     * Disable trust management and hostname verification. <i>NOTE</i> this is a security
     * hole, so only set this option if you cannot or do not want to verify the identity of the
     * host you are communicating with.
     */
    public HttpClientBuilder disableTrustManager() {
        this.disableTrustManager = true;
        return this;
    }

    /**
     * Disable cookie management.
     */
    public HttpClientBuilder disableCookies(boolean disable) {
        this.disableCookies = disable;
        return this;
    }

    /**
     * SSL policy used to verify hostnames
     *
     * @param policy
     * @return
     */
    public HttpClientBuilder hostnameVerification(HostnameVerificationPolicy policy) {
        this.policy = policy;
        return this;
    }


    public HttpClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public HttpClientBuilder trustStore(KeyStore truststore) {
        this.truststore = truststore;
        return this;
    }

    public HttpClientBuilder keyStore(KeyStore keyStore, String password) {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = password;
        return this;
    }

    public HttpClientBuilder keyStore(KeyStore keyStore, char[] password) {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = new String(password);
        return this;
    }

    public HttpClientBuilder proxyMappings(ProxyMappings proxyMappings) {
        this.proxyMappings = proxyMappings;
        return this;
    }

    public HttpClientBuilder expectContinueEnabled(boolean expectContinueEnabled) {
        this.expectContinueEnabled = expectContinueEnabled;
        return this;
    }

    public CloseableHttpClient build() {
        HostnameVerifier verifier = null;
        switch (policy) {
            case ANY:
                verifier = new NoopHostnameVerifier();
                break;
            case WILDCARD:
                verifier = new BrowserCompatHostnameVerifier();
                break;
            case STRICT:
                verifier = new StrictHostnameVerifier();
                break;
            case DEFAULT:
                verifier = new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault());
                break;
        }
        try {
            SSLConnectionSocketFactory sslsf = null;
            SSLContext theContext = sslContext;
            if (disableTrustManager) {
                theContext = SSLContext.getInstance("TLS");
                theContext.init(null, new TrustManager[]{new PassthroughTrustManager()},
                        new SecureRandom());
                verifier = new NoopHostnameVerifier();
                sslsf = new SSLConnectionSocketFactory(theContext, verifier);
            } else if (theContext != null) {
                sslsf = new SSLConnectionSocketFactory(theContext, verifier);
            } else if (clientKeyStore != null || truststore != null) {
                theContext = createSslContext("TLS", clientKeyStore, clientPrivateKeyPassword, truststore, null);
                sslsf = new SSLConnectionSocketFactory(theContext, verifier);
            } else {
                final SSLContext tlsContext = SSLContext.getInstance("TLS");
                tlsContext.init(null, null, null);
                sslsf = new SSLConnectionSocketFactory(tlsContext, verifier);
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout((int) TimeUnit.MILLISECONDS.convert(establishConnectionTimeout, establishConnectionTimeoutUnits))
                    .setSocketTimeout((int) TimeUnit.MILLISECONDS.convert(socketTimeout, socketTimeoutUnits))
                    .setExpectContinueEnabled(expectContinueEnabled).build();

            org.apache.http.impl.client.HttpClientBuilder builder = getApacheHttpClientBuilder()
                    .setDefaultRequestConfig(requestConfig)
                    .setSSLSocketFactory(sslsf)
                    .setMaxConnTotal(connectionPoolSize)
                    .setMaxConnPerRoute(maxPooledPerRoute)
                    .setConnectionTimeToLive(connectionTTL, connectionTTLUnit);

            if (!reuseConnections) {
                builder.setConnectionReuseStrategy(new NoConnectionReuseStrategy());
            }

            if (proxyMappings != null && !proxyMappings.isEmpty()) {
                builder.setRoutePlanner(new ProxyMappingsAwareRoutePlanner(proxyMappings));
            }

            if (maxConnectionIdleTime > 0) {
                // Will start background cleaner thread
                builder.evictIdleConnections(maxConnectionIdleTime, maxConnectionIdleTimeUnit);
            }

            if (disableCookies) builder.disableCookieManagement();

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected org.apache.http.impl.client.HttpClientBuilder getApacheHttpClientBuilder() {
        return HttpClients.custom();
    }

    private SSLContext createSslContext(
            final String algorithm,
            final KeyStore keystore,
            final String keyPassword,
            final KeyStore truststore,
            final SecureRandom random)
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        return SSLContexts.custom()
                        .setProtocol(algorithm)
                        .setSecureRandom(random)
                        .loadKeyMaterial(keystore, keyPassword != null ? keyPassword.toCharArray() : null)
                        .loadTrustMaterial(truststore, null)
                        .build();
    }

}
