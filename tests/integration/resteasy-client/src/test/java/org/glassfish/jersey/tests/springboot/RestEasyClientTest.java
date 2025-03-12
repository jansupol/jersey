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

package org.glassfish.jersey.tests.springboot;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;

import org.glassfish.jersey.jackson.internal.DefaultJacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class RestEasyClientTest extends JerseyTest {

    private static final CountDownLatch readFromLatch = new CountDownLatch(1);

    @Path("/")
    public static class RestEasyClientTestResource {
        @POST
        @Path("/test")
        @Produces(MediaType.APPLICATION_JSON)
        public String testPost(String echo) {
            return echo;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(RestEasyClientTestResource.class);
    }

    @Test
    public void test() throws InterruptedException {
        AtomicReference<String> messageRef = new AtomicReference<>();
        Logger logger = Logger.getLogger(DefaultJacksonJaxbJsonProvider.class.getName());
        logger.addHandler(new ConsoleHandler() {
            @Override
            public void publish(LogRecord record) {
                messageRef.set(record.getMessage());
            }
        });
        logger.setLevel(Level.FINE);

        final ResteasyClient client = new ResteasyClientBuilderImpl().build();

        client.register(TestDefaultJacksonJaxbJsonProvider.class);

        try (final Response r = client.target(target().getUri()).path("/test")
                .request().post(Entity.entity("{\"test\": \"test\"}", MediaType.APPLICATION_JSON))) {
            Object o = r.readEntity(Object.class);
            Assertions.assertTrue(o.toString().contains("test"));
            readFromLatch.await();
            Assertions.assertEquals(0, readFromLatch.getCount(), "DefaultJacksonJaxbJsonProvider has not been used");
        }

        client.close();
        MatcherAssert.assertThat(messageRef.get(), Matchers.notNullValue());

    }

    public static class TestDefaultJacksonJaxbJsonProvider extends DefaultJacksonJaxbJsonProvider {
        public TestDefaultJacksonJaxbJsonProvider(@Context Providers providers, @Context Configuration config) {
            super(providers, config);
        }

        @Override
        public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
            readFromLatch.countDown();
            return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        }
    }

}
