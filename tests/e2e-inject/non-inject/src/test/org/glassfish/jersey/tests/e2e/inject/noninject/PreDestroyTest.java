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

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class PreDestroyTest {
    private static final AtomicInteger disposeCounter = new AtomicInteger(0);
    private static final String HOST = "http://somewhere.anywhere";

    public interface ResponseObject {
        Response getResponse();
    }

    public static class ResponseObjectImpl implements ResponseObject {

        public ResponseObjectImpl() {

        }

        @PreDestroy
        public void preDestroy() {
            disposeCounter.incrementAndGet();
        }

        @Override
        public Response getResponse() {
            return Response.ok().build();
        }
    }

    private static class PreDestroyInjectingFilter implements ClientRequestFilter {
        private final ResponseObject responseSupplier;

        @Inject
        private PreDestroyInjectingFilter(ResponseObject responseSupplier) {
            this.responseSupplier = responseSupplier;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.abortWith(responseSupplier.getResponse());
        }
    }

    @Test
    public void testPreDestroyCount() {
        disposeCounter.set(0);
        int CNT = 4;
        Client client = ClientBuilder.newClient()
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(ResponseObjectImpl.class).to(ResponseObject.class)
                                .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
                    }
                }).register(PreDestroyInjectingFilter.class);

        for (int i = 0; i != CNT; i++) {
            try (Response response = client.target(HOST).request().get()) {
                MatcherAssert.assertThat(response.getStatus(), Matchers.is(200));
            }
        }

        MatcherAssert.assertThat(disposeCounter.get(), Matchers.is(1));
    }
}
