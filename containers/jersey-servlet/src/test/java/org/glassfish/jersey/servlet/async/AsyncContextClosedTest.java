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

package org.glassfish.jersey.servlet.async;

import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.servlet.internal.ResponseWriter;
import org.junit.jupiter.api.Test;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncContextClosedTest {
    @Test
    public void testClosedAsyncContext() {
        List<AsyncListener> asyncListeners = new ArrayList<>(1);
        AsyncContext async = (AsyncContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{AsyncContext.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        switch (method.getName()) {
                            case "addListener":
                                asyncListeners.add((AsyncListener) args[0]);
                                break;
                            case "complete":
                                asyncListeners.forEach((asyncListener -> {
                                    try {
                                        asyncListener.onComplete(null);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }));
                        }
                        return null;
                    }
                });

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{HttpServletRequest.class}, new InvocationHandler() {
                    boolean asyncStarted = false;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        switch (method.getName()) {
                            case "isAsyncStarted":
                                return asyncStarted;
                            case "startAsync":
                                asyncStarted = true;
                                return async;
                            case "getAsyncContext":
                                return async;

                        }
                        return null;
                    }
                });

        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{HttpServletResponse.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                });

        // Create writer
        ResponseWriter writer = new ResponseWriter(false, false, response,
                new AsyncContextDelegateProviderImpl().createDelegate(request, response),
                Executors.newSingleThreadScheduledExecutor());
        writer.suspend(10, TimeUnit.SECONDS, new ContainerResponseWriter.TimeoutHandler() {
            @Override
            public void onTimeout(ContainerResponseWriter responseWriter) {
                throw new IllegalStateException();
            }
        });
        // Simulate completion by the Servlet Container;
        request.getAsyncContext().complete();
        // Check write is ignored
        writer.writeResponseStatusAndHeaders(10, null);
    }
}
