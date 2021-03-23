/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.inject;

import org.glassfish.jersey.server.ServerProperties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SseAplication extends Application {

    @Path(InjectionChecker.ROOT)
    @ApplicationScoped
    public static class ApplicationScopedResource {
        @Context
        Sse contextSse;

        @Inject
        Sse injectSse;

        private volatile SseBroadcaster contextSseBroadcaster;
        private volatile SseBroadcaster injectSseBroadcaster;

        @GET
        @Path("register/{x}")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void register(@PathParam("x") String inject, @Context SseEventSink eventSink) {
            if (inject.contains("context")) {
                eventSink.send(contextSse.newEvent(inject));
                contextSseBroadcaster = contextSse.newBroadcaster();
                contextSseBroadcaster.register(eventSink);
            } else {
                eventSink.send(injectSse.newEvent(inject));
                injectSseBroadcaster = injectSse.newBroadcaster();
                injectSseBroadcaster.register(eventSink);
            }
        }

        @POST
        @Path("broadcast/{x}")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public void broadcast(@PathParam("x") String inject, String event) {
            if (inject.contains("context")) {
                contextSseBroadcaster.broadcast(contextSse.newEvent(event));
                contextSseBroadcaster.close();
            } else {
                injectSseBroadcaster.broadcast(injectSse.newEvent(event));
                injectSseBroadcaster.close();
            }
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(ApplicationScopedResource.class);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>(1);
        props.put(ServerProperties.WADL_FEATURE_DISABLE, true);
        return props;
    }
}
