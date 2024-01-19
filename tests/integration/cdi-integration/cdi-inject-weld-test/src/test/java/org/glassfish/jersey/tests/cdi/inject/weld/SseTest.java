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

package org.glassfish.jersey.tests.cdi.inject.weld;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseySingleContainerTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import javax.ws.rs.sse.SseEventSource;

public class SseTest extends JerseySingleContainerTest {
    protected static SeContainer container;
    protected InjectionManager injectionManager;

    @BeforeAll
    public static void setup() {
        SeContainerInitializer containerInitializer = SeContainerInitializer.newInstance();
        container = containerInitializer.initialize();
    }

    @AfterAll
    public static void weldTearDown() throws Exception {
        container.close();
    }

    @Path("/sse")
    public static class SseResource {
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void eventStream(@Context SseEventSink eventSink, @Context Sse sse) {
                try (SseEventSink sink = eventSink) {
                    eventSink.send(sse.newEvent("event1"));
                    eventSink.send(sse.newEvent("event2"));
                    eventSink.send(sse.newEvent("event3"));
                }
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(SseResource.class);
    }

    @Test
    public void sseBinderTest() {
        injectionManager = Injections.createInjectionManager();
        injectionManager.completeRegistration();

        Sse sse = injectionManager.getInstance(Sse.class);
        Assertions.assertEquals("org.glassfish.jersey.media.sse.internal.JerseySse", sse.getClass().getName());

        injectionManager.shutdown();
    }

    @Test
    public void sseTest() {
        WebTarget target = target("sse");
        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(System.out::println);
            source.open();
            Thread.sleep(500); // Consume events for just 500 ms
        } catch (InterruptedException e) {
        }
    }
}
