/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.netty.connector;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.innate.ClientProxy;

import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

class NettyConnectorConfiguration<N extends NettyConnectorConfiguration<N>> extends ConnectorConfiguration<N> {

    /* package */ NullableRef<NettyConnectionController> connectionController = NullableRef.empty();
    /* package */ NullableRef<Boolean> enableHostnameVerification = NullableRef.of(Boolean.TRUE);
    /* package */ NullableRef<Integer> expect100ContTimeout = NullableRef.of(
                                                                NettyClientProperties.DEFAULT_EXPECT_100_CONTINUE_TIMEOUT_VALUE);
    /* package */ NullableRef<Boolean> filterHeadersForProxy = NullableRef.of(Boolean.TRUE);
    /* package */ NullableRef<Integer> firstHttpHeaderLineLength = NullableRef.of(
                                                                NettyClientProperties.DEFAULT_INITIAL_LINE_LENGTH);
    /* package */ NullableRef<Integer> idleConnections = NullableRef.empty();
    /* package */ NullableRef<Integer> maxChunkSize = NullableRef.of(NettyClientProperties.DEFAULT_CHUNK_SIZE);
    /* package */ NullableRef<Integer> maxHeaderSize = NullableRef.of(NettyClientProperties.DEFAULT_HEADER_SIZE);
    // either from Jersey config, or default
    /* package */ NullableRef<Integer> maxPoolSizeTotal = NullableRef.of(DEFAULT_MAX_POOL_SIZE_TOTAL);
    // either from Jersey config, or default
    /* package */ NullableRef<Integer> maxPoolIdle = NullableRef.of(DEFAULT_MAX_POOL_IDLE);
    // either from system property, or from Jersey config, or default
    /* package */ NullableRef<Integer> maxPoolSize = NullableRef.of(HTTP_KEEPALIVE ? MAX_POOL_SIZE : DEFAULT_MAX_POOL_SIZE);
    /* package */ NullableRef<Integer> maxRedirects = NullableRef.of(DEFAULT_MAX_REDIRECTS);
    /* package */ NullableRef<Boolean> preserveMethodOnRedirect = NullableRef.of(Boolean.TRUE);
    /* package */ NullableRef<NettyHttpRedirectController> redirectController = NullableRef.empty();



    // If HTTP keepalive is enabled the value of "http.maxConnections" determines the maximum number
    // of idle connections that will be simultaneously kept alive, per destination.
    private static final String HTTP_KEEPALIVE_STRING = System.getProperty("http.keepAlive");
    // http.keepalive (default: true)
    private static final Boolean HTTP_KEEPALIVE =
            HTTP_KEEPALIVE_STRING == null ? Boolean.TRUE : Boolean.parseBoolean(HTTP_KEEPALIVE_STRING);

    // http.maxConnections (default: 5)
    private static final int DEFAULT_MAX_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = Integer.getInteger("http.maxConnections", DEFAULT_MAX_POOL_SIZE);
    private static final int DEFAULT_MAX_POOL_IDLE = 60; // seconds
    private static final int DEFAULT_MAX_POOL_SIZE_TOTAL = 60; // connections

    private static final int DEFAULT_MAX_REDIRECTS = 5;

    @Override
    protected void setNonEmpty(N other) {
        super.setNonEmpty(other);
        this.connectionController.setNonEmpty(other.connectionController);
        this.enableHostnameVerification.setNonEmpty(other.enableHostnameVerification);
        this.expect100ContTimeout.setNonEmpty(other.expect100ContTimeout);
        this.filterHeadersForProxy.setNonEmpty(other.filterHeadersForProxy);
        this.firstHttpHeaderLineLength.setNonEmpty(other.firstHttpHeaderLineLength);
        this.idleConnections.setNonEmpty(other.idleConnections);
        this.maxChunkSize.setNonEmpty(other.maxChunkSize);
        this.maxHeaderSize.setNonEmpty(other.maxHeaderSize);
        this.maxPoolIdle.setNonEmpty(other.maxPoolIdle);
        this.maxPoolSize.setNonEmpty(other.maxPoolSize);
        this.maxPoolSizeTotal.setNonEmpty(other.maxPoolSizeTotal);
        this.maxRedirects.setNonEmpty(other.maxRedirects);
        this.preserveMethodOnRedirect.setNonEmpty(other.preserveMethodOnRedirect);
        this.redirectController.setNonEmpty(other.redirectController);
    }

    /**
     * Set the connection pooling controller for the Netty Connector.
     *
     * @param controller the connection pooling controller.
     * @return updated configuration.
     */
    public N connectorController(NettyConnectionController controller) {
        connectionController.set(controller);
        return self();
    }

    /* package */ NettyConnectionController connectionController() {
        return connectionController.isPresent() ? connectionController.get() : new NettyConnectionController();
    }

    /**
     * This setting determines waiting time in milliseconds for 100-Continue response when 100-Continue is sent by the client.
     * The property {@link NettyClientProperties#EXPECT_100_CONTINUE_TIMEOUT} has precedence over this setting.
     *
     * @param millis the timeout for 100-Continue response.
     * @return updated configuration.
     */
    public N expect100ContinueTimeout(int millis) {
        expect100ContTimeout.set(millis);
        return self();
    }

    /**
     * Update {@link #expect100ContinueTimeout(int) expect 100-Continue timeout} based on current http request properties.
     *
     * @param clientRequest the current http client request.
     * @return updated configuration.
     */
    public N expect100ContinueTimeout(ClientRequest clientRequest) {
        expect100ContTimeout.set(clientRequest.resolveProperty(
                NettyClientProperties.EXPECT_100_CONTINUE_TIMEOUT,
                expect100ContTimeout.get()));
        return self();
    }

    /**
     * Enable or disable the endpoint identification algorithm to HTTPS. The property
     * {@link NettyClientProperties#ENABLE_SSL_HOSTNAME_VERIFICATION} has  over this setting.
     *
     * @param enable enable or disable the hostname verification.
     * @return updated configuration.
     */
    public N enableSslHostnameVerification(boolean enable) {
        enableHostnameVerification.set(enable);
        return self();
    }

    /**
     * This property determines the number of seconds the idle connections are kept in the pool before pruned.
     * The default is 60. Specify 0 to disable. The property {@link NettyClientProperties#IDLE_CONNECTION_PRUNE_TIMEOUT}
     * has precedence over this setting.
     *
     * @param seconds the timeout in seconds.
     * @return updated configuration.
     */
    public N idleConnectionPruneTimeout(int seconds) {
        maxPoolIdle.set(seconds);
        return self();
    }

    /**
     * Set the maximum length of the first line of the HTTP header.
     * The property {@link NettyClientProperties#MAX_INITIAL_LINE_LENGTH} has precedence over this setting.
     *
     * @param length the length of the first line of the HTTP header.
     * @return updated configuration.
     */
    public N initialHttpHeaderLineLength(int length) {
        firstHttpHeaderLineLength.set(length);
        return self();
    }

    /**
     * Set the maximum chunk size for the Netty connector. The property {@link NettyClientProperties#MAX_CHUNK_SIZE}
     * has precedence over this setting.
     *
     * @param size the new size of chunks.
     * @return updated configuration.
     */
    public N maxChunkSize(int size) {
        maxChunkSize.set(size);
        return self();
    }

    /**
     * This setting determines the maximum number of idle connections that will be simultaneously kept alive, per destination.
     * The default is 5. The property {@link NettyClientProperties#MAX_CONNECTIONS} takes precedence over this setting.
     *
     * @param maxCount maximum number of idle connections per destination.
     * @return updated configuration.
     */
    public N maxConnectionsPerDestination(int maxCount) {
        idleConnections.set(maxCount);
        return self();
    }

    /**
     * Set the maximum header size in bytes for the HTTP headers processed by Netty.
     * The property {@link NettyClientProperties#MAX_HEADER_SIZE} has precedence over this setting.
     *
     * @param size the new maximum header size.
     * @return updated configuration.
     */
    public N maxHeaderSize(int size) {
        maxHeaderSize.set(size);
        return self();
    }

    /**
     * Set the maximum number of redirects to prevent infinite redirect loop. The default is 5.
     * The property {@link NettyClientProperties#MAX_REDIRECTS} has precedence over this setting.
     *
     * @param max the maximum number of redirects.
     * @return updated configuration.
     */
    public N maxRedirects(int max) {
        maxRedirects.set(max);
        return self();
    }

    /**
     * Update {@link #maxRedirects(int)} value from the HTTP Client request.
     * @param request the HTTP Client request.
     * @return maximum redirects value.
     */
    /* package */ int maxRedirects(ClientRequest request) {
//        maxRedirects.ifEmptySet(DEFAULT_MAX_REDIRECTS);
        maxRedirects.set(request.resolveProperty(NettyClientProperties.MAX_REDIRECTS, maxRedirects.get()));
        return maxRedirects.get();
    }

    /**
     * Set the preservation of methods during HTTP redirect.
     * By default, the HTTP POST request are not transformed into HTTP GET for status 301 & 302.
     * The property {@link NettyClientProperties#PRESERVE_METHOD_ON_REDIRECT} has precedence over this setting.
     *
     * @param preserve to preserve or not to preserve.
     * @return updated configuration.
     */
    public N preserveMethodOnRedirect(boolean preserve) {
        preserveMethodOnRedirect.set(preserve);
        return self();
    }

    /**
     * Update the {@link #preserveMethodOnRedirect(boolean) preservation} of HTTP method during HTTP redirect
     * by HTTP client request properties.
     *
     * @param request HTTP client request.
     * @return the value of preservation.
     */
    /* package */ boolean preserveMethodOnRedirect(ClientRequest request) {
        preserveMethodOnRedirect.set(
                request.resolveProperty(NettyClientProperties.PRESERVE_METHOD_ON_REDIRECT, preserveMethodOnRedirect.get()));
        return preserveMethodOnRedirect.get();
    }

    /**
     * Set the Netty Connector HTTP redirect controller.
     * The property {@link NettyClientProperties#HTTP_REDIRECT_CONTROLLER} has precedence over this setting.
     *
     * @param controller the HTTP redirect controller.
     * @return updated configuration.
     */
    public N redirectController(NettyHttpRedirectController controller) {
        redirectController.set(controller);
        return self();
    }

    /* package */ NettyHttpRedirectController redirectController(ClientRequest request) {
        NettyHttpRedirectController customRedirectController =
                request.resolveProperty(NettyClientProperties.HTTP_REDIRECT_CONTROLLER, NettyHttpRedirectController.class);
        if (customRedirectController == null) {
            customRedirectController = redirectController.get();
        }
        if (customRedirectController == null) {
            customRedirectController = new NettyHttpRedirectController();
        }

        return customRedirectController;
    }

    /**
     * Set the maximum number of idle connections that will be simultaneously kept alive. The property
     * {@link NettyClientProperties#MAX_CONNECTIONS_TOTAL} has precedence over this setting.
     *
     * @param max the maximum number of idle connections.
     * @return updated configuration.
     */
    public N maxTotalConnection(int max) {
        maxPoolSizeTotal.set(max);
        return self();
    }


    /**
     * <p>
     *  Return a new instance of configuration updated by the merged settings from this and client properties.
     *  Only properties unresolved during the request are update.
     * </p><p>
     *  {@code This} is meant to be settings from the connector.
     *  The priorities should go DEFAULTS -> SYSTEM -> CONNECTOR -> CLIENT -> REQUEST
     * </p>
     *
     * @param client the REST client.
     * @return a new instance of configuration.
     */
    /* package */ N fromClient(Client client) {
        final Map<String, Object> properties = client.getConfiguration().getProperties();
        final N clientConfiguration = copy();

        final Object threadPoolSize = properties.get(ClientProperties.ASYNC_THREADPOOL_SIZE);
        if (threadPoolSize instanceof Integer && (Integer) threadPoolSize > 0) {
            clientConfiguration.asyncThreadPoolSize((Integer) threadPoolSize);
        }

        final Object maxPoolSizeTotalProperty = properties.get(NettyClientProperties.MAX_CONNECTIONS_TOTAL);
        final Object maxPoolIdleProperty = properties.get(NettyClientProperties.IDLE_CONNECTION_PRUNE_TIMEOUT);
        final Object maxPoolSizeProperty = properties.get(NettyClientProperties.MAX_CONNECTIONS);

        if (maxPoolSizeTotalProperty != null) {
            clientConfiguration.maxPoolSizeTotal.set((Integer) maxPoolSizeTotalProperty);
        }

        if (maxPoolIdleProperty != null) {
            clientConfiguration.maxPoolIdle.set((Integer) maxPoolIdleProperty);
        }

        if (maxPoolSizeProperty != null) {
            clientConfiguration.maxPoolSize.set((Integer) maxPoolSizeProperty);
        }

        if (clientConfiguration.maxPoolSizeTotal.get() < 0) {
            throw new ProcessingException(LocalizationMessages.WRONG_MAX_POOL_TOTAL(maxPoolSizeTotal.get()));
        }

        if (clientConfiguration.maxPoolSize.get() < 0) {
            throw new ProcessingException(LocalizationMessages.WRONG_MAX_POOL_SIZE(maxPoolSize.get()));
        }

        return clientConfiguration;
    }

    /* package */ HttpClientCodec createHttpClientCodec(Map<String, Object> properties) {
        firstHttpHeaderLineLength.set(ClientProperties.getValue(properties,
                NettyClientProperties.MAX_INITIAL_LINE_LENGTH, firstHttpHeaderLineLength.get()));
        maxHeaderSize.set(ClientProperties.getValue(properties, NettyClientProperties.MAX_HEADER_SIZE, maxHeaderSize.get()));
        maxChunkSize.set(ClientProperties.getValue(properties, NettyClientProperties.MAX_CHUNK_SIZE, maxChunkSize.get()));

        return new HttpClientCodec(firstHttpHeaderLineLength.get(), maxHeaderSize.get(), maxChunkSize.get());
    }

    /* package */ ProxyHandler createProxyHandler(ClientProxy clientProxy, ClientRequest jerseyRequest) {
        final URI u = clientProxy.uri();
        InetSocketAddress proxyAddr = new InetSocketAddress(u.getHost(), u.getPort() == -1 ? 8080 : u.getPort());

        final Boolean filter = jerseyRequest
                .resolveProperty(NettyClientProperties.FILTER_HEADERS_FOR_PROXY, filterHeadersForProxy.get());
        HttpHeaders httpHeaders = NettyConnector.setHeaders(jerseyRequest, new DefaultHttpHeaders(), Boolean.TRUE.equals(filter));

        ProxyHandler proxy = clientProxy.userName() == null ? new HttpProxyHandler(proxyAddr, httpHeaders)
                : new HttpProxyHandler(proxyAddr, clientProxy.userName(), clientProxy.password(), httpHeaders);
        if (connectTimeout.get() > 0) {
            proxy.setConnectTimeoutMillis(connectTimeout.get());
        }

        return proxy;
    }

    /* package */ boolean isSslHostnameVerificationEnabled(Map<String, Object> properties) {
        return ClientProperties.getValue(properties,
                NettyClientProperties.ENABLE_SSL_HOSTNAME_VERIFICATION,
                enableHostnameVerification.get());
    }

    /* package */ SSLContext getSslContext(Client client, ClientRequest request) {
        return super.sslContext(client, request);
    }


    @Override
    @SuppressWarnings("unchecked")
    protected N self() {
        return (N) this;
    }

    @Override
    protected N instance() {
        return new NettyConnectorConfiguration<N>().self();
    }

}
