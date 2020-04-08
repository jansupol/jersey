/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.IOException;

public class ExceptionMapperInResourceConstructorTest extends JerseyTest {

    public static class ResourceConstructorExceptionMapper implements ExceptionMapper<IOException> {
        @Override
        public Response toResponse(IOException exception) {
            return Response.ok(exception.getMessage()).build();
        }
    }

    @Path("/")
    public static class ResourceWithExceptionInConstructor {
        public static final String MSG = "Exception from constructor";
        public ResourceWithExceptionInConstructor() throws IOException {
            throw new IOException(MSG);
        }

        @GET
        public String get() {
           return "another message";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ResourceWithExceptionInConstructor.class, ResourceConstructorExceptionMapper.class);
    }

    @Test
    public void exceptionMapperInConstructorIsUsed() {
        String msg = target().request().get(String.class);
        Assert.assertEquals(ResourceWithExceptionInConstructor.MSG, msg);
    }
}
