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

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.innate.ClientProxy;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

// TODO move to client/common
public class ConnectorConfiguration<E extends ConnectorConfiguration<E>> {
    /* package */ NullableRef<Integer> connectTimeout = NullableRef.of(0);
    /* package */ NullableRef<Boolean> expect100Continue = NullableRef.empty();
    /* package */ NullableRef<Long> expect100continueThreshold = NullableRef.of(
                                                ClientProperties.DEFAULT_EXPECT_100_CONTINUE_THRESHOLD_SIZE);
    /* package */ NullableRef<Boolean> followRedirects = NullableRef.of(Boolean.TRUE);
    /* package */ NullableRef<Object> proxyUri = NullableRef.empty();
    /* package */ NullableRef<String> proxyUserName = NullableRef.empty();
    /* package */ NullableRef<String> proxyPassword = NullableRef.empty();
    /* package */ NullableRef<Integer> readTimeout = NullableRef.of(0);
    /* package */ NullableRef<RequestEntityProcessing> requestEntityProcessing = NullableRef.empty();
    /* package */ NullableRef<Supplier<SSLContext>> sslContextSupplier = NullableRef.empty();
    /* package */ NullableRef<Integer> threadPoolSize = NullableRef.empty();

    protected E copy() {
        E config = instance();
        config.setNonEmpty(self());
        return config;
    }

    protected void setNonEmpty(E other) {
        this.connectTimeout.setNonEmpty(other.connectTimeout);
        this.expect100Continue.setNonEmpty(other.expect100Continue);
        this.expect100continueThreshold.setNonEmpty(other.expect100continueThreshold);
        this.followRedirects.setNonEmpty(other.followRedirects);
        this.proxyUri.setNonEmpty(other.proxyUri);
        this.proxyUserName.setNonEmpty(other.proxyUserName);
        this.proxyPassword.setNonEmpty(other.proxyPassword);
        this.readTimeout.setNonEmpty(other.readTimeout);
        this.requestEntityProcessing.setNonEmpty(other.requestEntityProcessing);
        this.sslContextSupplier.setNonEmpty(other.sslContextSupplier);
        this.threadPoolSize.setNonEmpty(other.threadPoolSize);
    }

    /**
     * Set the asynchronous thread-pool size. The property {@link ClientProperties#ASYNC_THREADPOOL_SIZE}
     * has precedence over this setting.
     *
     * @param threadPoolSize the size of the asynchronous thread-pool.
     * @return updated configuration.
     */
    public E asyncThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize.set(threadPoolSize);
        return self();
    }

    /**
     * Set connect timeout. The property {@link ClientProperties#CONNECT_TIMEOUT}
     * has precedence over this setting.
     *
     * @param millis timeout in milliseconds.
     * @return updated configuration.
     */
    public E connectTimeout(int millis) {
        connectTimeout.set(millis);
        return self();
    }

    /**
     * Update connect timeout value based on request properties settings.
     *
     * @param request the current HTTP client request.
     * @return the connect timeout.
     */
    protected int connectTimeout(ClientRequest request) {
        connectTimeout.set(request.resolveProperty(ClientProperties.CONNECT_TIMEOUT, connectTimeout.get()));
        return connectTimeout.get();
    }

    /**
     * Allows for HTTP Expect:100-Continue.
     * The property {@link ClientProperties#EXPECT_100_CONTINUE} has precedence over this setting.
     *
     * @param enable allows for HTTP Expect:100-Continue or not.
     * @return updated configuration.
     */
    public E expect100Continue(boolean enable) {
        expect100Continue.set(enable);
        return self();
    }

    /**
     * Update the {@link #expect100Continue(boolean)} from the HTTP client request.
     *
     * @param request the HTTP client request.
     * @return the Expect: 100-Continue support value.
     */
    protected Boolean expect100Continue(ClientRequest request) {
        final Boolean expectContinueActivated = request.resolveProperty(ClientProperties.EXPECT_100_CONTINUE, Boolean.class);
        if (expectContinueActivated != null) {
            expect100Continue.set(expectContinueActivated);
        }
        return expect100Continue.get();
    }

    /**
     * Set the Expect:100-Continue content-length threshold size.
     * The {@link ClientProperties#EXPECT_100_CONTINUE_THRESHOLD_SIZE} property has precedence over this setting.
     *
     * @param size the content-length threshold.
     * @return updated configuration.
     */
    public E expect100ContinueThreshold(long size) {
        expect100ContinueThreshold(size);
        return self();
    }

    /**
     * Update the {@link #expect100ContinueThreshold(long)} from the HTTP client request.
     *
     * @param request the HTTP client request.
     * @return the content length threshold size.
     */
    protected long expect100ContinueThreshold(ClientRequest request) {
        expect100continueThreshold.set(request.resolveProperty(
                ClientProperties.EXPECT_100_CONTINUE_THRESHOLD_SIZE,
                expect100continueThreshold.get()));
        return expect100continueThreshold.get();
    }

    /**
     * Set to follow redirects. The property {@link ClientProperties#FOLLOW_REDIRECTS} has precedence over this setting.
     *
     * @param follow to follow or not to follow.
     * @return updated configuration.
     */
    public E followRedirects(boolean follow) {
        followRedirects.set(follow);
        return self();
    }

    /**
     * Update the {@link #followRedirects(boolean)} setting from the HTTP client request. The default is {@code true}.
     *
     * @param request the HTTP client request.
     * @return updated configuration.
     */
    protected boolean followRedirects(ClientRequest request) {
        followRedirects.set(request.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, followRedirects.get()));
        return followRedirects.get();
    }

    /**
     * Set proxy password. The property {@link ClientProperties#PROXY_PASSWORD}
     * has precedence over this setting.
     *
     * @param proxyPassword the proxy password.
     * @return updated configuration.
     */
    public E proxyPassword(String proxyPassword) {
        this.proxyPassword.set(proxyPassword);
        return self();
    }

    /**
     * Set proxy username. The property {@link ClientProperties#PROXY_USERNAME}
     * has precedence over this setting.
     *
     * @param userName the proxy username.
     * @return updated configuration.
     */
    public E proxyUserName(String userName) {
        proxyUserName.set(userName);
        return self();
    }

    /**
     * Set proxy URI. The property {@link ClientProperties#PROXY_URI}
     * has precedence over this setting.
     *
     * @param proxyUri the proxy URI.
     * @return updated configuration.
     */
    public E proxyUri(String proxyUri) {
        this.proxyUri.set(proxyUri);
        return self();
    }

    /**
     * Set proxy URI. The property {@link ClientProperties#PROXY_URI}
     * has precedence over this setting.
     *
     * @param proxyUri the proxy URI.
     * @return updated configuration.
     */
    public E proxyUri(URI proxyUri) {
        this.proxyUri.set(proxyUri);
        return self();
    }

    /**
     * Set HTTP proxy. The property {@link ClientProperties#PROXY_URI}
     * has precedence over this setting.
     *
     * @param proxy the HTTP proxy.
     * @return updated configuration.
     */
    public E proxy(Proxy proxy) {
        this.proxyUri.set(proxy);
        return self();
    }

    protected Optional<ClientProxy> proxy(ClientRequest request, URI requestUri) {
        Optional<ClientProxy> proxy = ClientProxy.proxyFromRequest(request);
        if (!proxy.isPresent() && proxyUri.isPresent()) {
            // TODO support in ClientProxy
            Map<String, Object> properties = new HashMap<>();
            properties.put(ClientProperties.PROXY_URI, proxyUri.get());
            properties.put(ClientProperties.PROXY_USERNAME, proxyUserName.get());
            properties.put(ClientProperties.PROXY_PASSWORD, proxyPassword.get());
            Configuration configuration = (Configuration) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{Configuration.class}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            switch (method.getName()) {
                                case "getProperties":
                                    return properties;
                            }
                            return null;
                        }
                    });
            proxy = ClientProxy.proxyFromConfiguration(configuration);
        }
        if (!proxy.isPresent()) {
            proxy = ClientProxy.proxyFromProperties(requestUri);
        }
        return proxy;
    }

    /**
     * Set read timeout. The property {@link ClientProperties#READ_TIMEOUT}
     * has precedence over this setting.
     *
     * @param millis timeout in milliseconds.
     * @return updated configuration.
     */
    public E readTimeout(int millis) {
        readTimeout.set(millis);
        return self();
    }

    /**
     * Update {@link #readTimeout(int) read timeout} based on the HTTP request properties.
     *
     * @param request the current HTTP client request.
     * @return updated configuration.
     */
    protected E readTimeout(ClientRequest request) {
        readTimeout.set(request.resolveProperty(ClientProperties.READ_TIMEOUT, readTimeout.get()));
        return self();
    }

    public E requestEntityProcessing(RequestEntityProcessing requestEntityProcessing) {
        this.requestEntityProcessing.set(requestEntityProcessing);
        return self();
    }

    /**
     * Get the {@link RequestEntityProcessing} updated by the HTTP client request.
     *
     * @param request the HTTP client request.
     * @return the RequestEntityProcessing type.
     */
    protected RequestEntityProcessing requestEntityProcessing(ClientRequest request) {
        RequestEntityProcessing entityProcessing = request.resolveProperty(
                ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.class);
        if (entityProcessing == null) {
            entityProcessing = requestEntityProcessing.get();
        }
        return entityProcessing;
    }

    /**
     * Get {@link SSLContext} either from the {@link ClientProperties#SSL_CONTEXT_SUPPLIER}, or from this configuration,
     * or from the {@link Client#getSslContext()} in this order.
     *
     * @param client the client used to get the {@link SSLContext}.
     * @param request the request used to get the {@link SSLContext}.
     * @return the {@link SSLContext}.
     */
    protected SSLContext sslContext(Client client, ClientRequest request) {
        Supplier<SSLContext> supplier = request.resolveProperty(ClientProperties.SSL_CONTEXT_SUPPLIER, Supplier.class);
        if (supplier == null) {
            supplier = sslContextSupplier.get();
        }
        return supplier == null ? client.getSslContext() : supplier.get();
    }

    /**
     * Set the {@link SSLContext} supplier. The property {@link ClientProperties#SSL_CONTEXT_SUPPLIER} has precedence over
     * this setting.
     *
     * @param sslContextSupplier the {@link SSLContext} supplier.
     * @return the updated configuration.
     */
    public E sslContextSupplier(Supplier<SSLContext> sslContextSupplier) {
        this.sslContextSupplier.set(sslContextSupplier);
        return self();
    }

    protected E instance() {
        return new ConnectorConfiguration<E>().self();
    }

    @SuppressWarnings("unchecked")
    protected E self() {
        return (E) this;
    }

    /**
     * <p>
     * A reference to a value. The reference can be empty, but unlike the {@code Optional}, once a value is set,
     * it never can be empty again. The {@code null} value is treated as a non-empty value of null.
     * </p><p>
     * This {@code null}
     * can be used to override some previous configuration value, to distinguish the intentional {@code null} override
     * from an empty (non-set) configuration value.
     * </p>
     * @param <T> type of the value.
     */
    protected static class NullableRef<T> implements org.glassfish.jersey.internal.util.collection.Ref<T> {

        private NullableRef() {
            // use factory methods;
        }

        public static <T> NullableRef<T> empty() {
            return new NullableRef<>();
        }

        public static <T> NullableRef<T> of(T value) {
            NullableRef<T> ref = new NullableRef<>();
            ref.set(value);
            return ref;
        }

        private boolean empty = true;
        private T ref = null;

        @Override
        public void set(T value) {
            empty = false;
            ref = value;
        }

        void setNonEmpty(NullableRef<T> other) {
            other.ifPresent(this::set);
        }

        @Override
        public T get() {
            return ref;
        }

        /**
         * Run action if and only if the condition applies.
         *
         * @param predicate the condition to be met.
         * @param action the action to run if condition is met.
         */
        public void iff(Predicate<T> predicate, Runnable action) {
            if (predicate.test(ref)) {
                action.run();
            }
        }

        /**
         * If it is empty, sets the {@code value} value. Keeps the original value, otherwise.
         *
         * @param value the value to be set if empty.
         */
        public void ifEmptySet(T value) {
            if (empty) {
                set(value);
            }
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise does nothing.
         *
         * @param action the action to be performed, if a value is present
         * @throws NullPointerException if value is present and the given action is
         *         {@code null}
         */
        public void ifPresent(Consumer<? super T> action) {
            if (!empty) {
                action.accept(ref);
            }
        }

        /**
         * If a value is present, performs the given action with the value,
         * otherwise performs the given empty-based action.
         *
         * @param action the action to be performed, if a value is present
         * @param emptyAction the empty-based action to be performed, if no value is
         *        present
         * @throws NullPointerException if a value is present and the given action
         *         is {@code null}, or no value is present and the given empty-based
         *         action is {@code null}.
         */
        public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
            if (!empty) {
                action.accept(ref);
            } else {
                emptyAction.run();
            }
        }

        /**
         * If a value is  not present, returns {@code true}, otherwise
         * {@code false}.
         *
         * @return  {@code true} if a value is not present, otherwise {@code false}
         */
        public boolean isEmpty() {
            return empty;
        }

        /**
         * If a value is present, returns {@code true}, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return !empty;
        }


        @Override
        public int hashCode() {
            return Objects.hash(ref, empty);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof NullableRef)) {
                return false;
            }

            NullableRef<?> that = (NullableRef<?>) o;
            return Objects.equals(empty, that.empty) && Objects.equals(ref, that.ref);
        }

        @Override
        public String toString() {
            return empty ? "<empty>" : ref == null ? "<null>" : ref.toString();
        }
    }
}
