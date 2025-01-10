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

package org.glassfish.jersey.netty.connector;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class HostHeaderTest extends JerseyTest {

    private static final String HTTP_HEADER_NAME = "HTTP_PORT_INT";

    @Path("/")
    public static class HostHeaderTestEchoResource {

        @POST
        public String post(@Context HttpHeaders headers) {
            return get(headers);
        }

        @GET
        public String get(@Context HttpHeaders headers) {
            String sPort = headers.getHeaderString(HTTP_HEADER_NAME);
            String hostPort = headers.getHeaderString(HttpHeaders.HOST);
            int indexColon = hostPort.indexOf(':');
            if (indexColon != -1) {
                hostPort = hostPort.substring(indexColon + 1);
            }
            if (sPort.equals(hostPort.trim())) {
                return GET.class.getName();
            } else {
                return "Expected port " + sPort + " but found " + hostPort;
            }
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(HostHeaderTestEchoResource.class);
    }

    @Test
    public void testHostHeaderAndPort() {
        int port = getPort();
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new NettyConnectorProvider());
        try (Response response = ClientBuilder.newClient(config).target(target().getUri())
                .request()
                .header(HTTP_HEADER_NAME, port)
                .get()) {
            MatcherAssert.assertThat(response.getStatus(), Matchers.is(200));
            MatcherAssert.assertThat(response.readEntity(String.class), Matchers.is(GET.class.getName()));
        }
    }

    @Test
    public void testHostHeaderAndPortAfterRemovedFromFilter() {
        int port = getPort();
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new NettyConnectorProvider());
        try (Response response = ClientBuilder.newClient(config)
                .target(target().getUri())
                .request()
                .header(HTTP_HEADER_NAME, port)
                .post(Entity.entity("xxx", MediaType.TEXT_PLAIN_TYPE))) {
            MatcherAssert.assertThat(response.getStatus(), Matchers.is(200));
            MatcherAssert.assertThat(response.readEntity(String.class), Matchers.is(GET.class.getName()));
        }
    }

}
