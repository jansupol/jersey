/*
 * Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.inject.noninject;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.innate.inject.NonInjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InstanceListSizeTest {

    private static final String TEST_HEADER = "TEST";
    private static final String HOST = "https://anywhere.any";

    @Test
    public void leakTest() throws ExecutionException, InterruptedException {
        int CNT = 100;
        Client client = ClientBuilder.newClient();
        client.register(InjectionManagerGrowChecker.class);
        Response response = client.target(HOST).request().header(TEST_HEADER, "0").get();
        int status = response.getStatus();
        response.close();
        //Create instance in NonInjectionManager$TypedInstances.threadPredestroyables

        for (int i = 0; i <= CNT; i++) {
            final String header = String.valueOf(i + 1);
            try (Response r = client.target(HOST).request()
                    .header(TEST_HEADER, header)
                    .async()
                    .post(Entity.text("text")).get()) {
                int stat = r.getStatus();
                MatcherAssert.assertThat(
                        "NonInjectionManager#Types#disposableSupplierObjects is increasing", stat, Matchers.is(202));
            }
        }
        //Create 10 instance in NonInjectionManager$TypedInstances.threadPredestroyables

        for (int i = 0; i <= CNT; i++) {
            final String header = String.valueOf(i + CNT + 2);
            final Object text = CompletableFuture.supplyAsync(() -> {
                Response test = client.target(HOST).request()
                        .header("TEST", header)
                        .post(Entity.text("text"));
                int stat = test.getStatus();
                test.close();
                MatcherAssert.assertThat(
                        "NonInjectionManager#Types#disposableSupplierObjects is increasing", stat, Matchers.is(202));

                return null;
            }).join();
        }
        //Create 10 instance in NonInjectionManager$TypedInstances.threadPredestroyables

        response = client.target(HOST).request().header(TEST_HEADER,  2 * CNT + 3).get();
        status = response.getStatus();
        MatcherAssert.assertThat(status, Matchers.is(202));
        response.close();
    }

    private static class InjectionManagerGrowChecker implements ClientRequestFilter {
        private boolean first = true;
        private int disposableSize = 0;
        private int threadInstancesSize = 0;
        private HttpHeaders headers;
        private int headerCnt = 0;

        @Inject
        public InjectionManagerGrowChecker(HttpHeaders headers) {
            this.headers = headers;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            Response.Status status = Response.Status.ACCEPTED;
            if (headerCnt++ != Integer.parseInt(headers.getHeaderString("TEST"))) {
                status = Response.Status.BAD_REQUEST;
            }

            NonInjectionManager nonInjectionManager = getInjectionManager(requestContext);
            Object types = getDeclaredField(nonInjectionManager, "types");
            Object instances = getDeclaredField(nonInjectionManager, "instances");
            if (first) {
                first = false;
                disposableSize = getThreadInstances(types, "disposableSupplierObjects")
                        + getThreadInstances(instances, "disposableSupplierObjects");
                threadInstancesSize = getThreadInstances(types, "threadInstances")
                        + getThreadInstances(instances, "threadInstances");
            } else {
                int newPredestroyableSize = getThreadInstances(types, "disposableSupplierObjects")
                        + getThreadInstances(instances, "disposableSupplierObjects");
                if (newPredestroyableSize > disposableSize + 1 /* a new service to get disposed */) {
                    status = Response.Status.EXPECTATION_FAILED;
                }
                int newThreadInstances = getThreadInstances(types, "threadInstances")
                        + getThreadInstances(instances, "threadInstances");
                if (newThreadInstances > threadInstancesSize) {
                    status = Response.Status.PRECONDITION_FAILED;
                }
            }

            requestContext.abortWith(Response.status(status).build());
        }
    }

    private static NonInjectionManager getInjectionManager(ClientRequestContext context) {
        ClientRequest request = ((ClientRequest) context);
        try {
            Method clientConfigMethod = ClientRequest.class.getDeclaredMethod("getClientConfig");
            clientConfigMethod.setAccessible(true);
            ClientConfig clientConfig = (ClientConfig) clientConfigMethod.invoke(request);

            Method runtimeMethod = ClientConfig.class.getDeclaredMethod("getRuntime");
            runtimeMethod.setAccessible(true);
            Object clientRuntime = runtimeMethod.invoke(clientConfig);
            Class<?> clientRuntimeClass = clientRuntime.getClass();

            Method injectionManagerMethod = clientRuntimeClass.getDeclaredMethod("getInjectionManager");
            injectionManagerMethod.setAccessible(true);
            InjectionManager injectionManager = (InjectionManager) injectionManagerMethod.invoke(clientRuntime);
            return (NonInjectionManager) injectionManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getDeclaredField(NonInjectionManager nonInjectionManager, String name) {
        try {
            Field typesField = NonInjectionManager.class.getDeclaredField(name);
            typesField.setAccessible(true);
            return typesField.get(nonInjectionManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int getThreadInstances(Object typedInstances, String threadLocalName) {
        try {
            Field threadLocalField =
                    typedInstances.getClass().getSuperclass().getDeclaredField(threadLocalName);
            threadLocalField.setAccessible(true);
            ThreadLocal<MultivaluedMap<?,?>> threadLocal =
                    (ThreadLocal<MultivaluedMap<?, ?>>) threadLocalField.get(typedInstances);
            MultivaluedMap<?, ?> map = threadLocal.get();
            if (map == null) {
                return 0;
            } else {
                int cnt = 0;
                Set<? extends Map.Entry<?, ? extends List<?>>> set = map.entrySet();
                for (Map.Entry<?, ? extends List<?>> entry : map.entrySet()) {
                    cnt += entry.getValue().size();
                }
                return cnt;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
