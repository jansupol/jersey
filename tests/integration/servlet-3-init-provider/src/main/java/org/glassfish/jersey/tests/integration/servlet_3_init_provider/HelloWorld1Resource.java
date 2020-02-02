/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.servlet_3_init_provider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * @author Libor Kramolis
 */
@Path("helloworld1")
public class HelloWorld1Resource extends AbstractHelloWorldResource {

    @Override
    protected String createName() {
        return "World #1";
    }

    @GET
    @Path("servlets")
    public int getServletsCount() {
        return TestServletContainerProvider.getServletNames().size();
    }

    @GET
    @Path("servlets/{name}")
    public boolean hasServletName(@PathParam("name") String servletName) {
        return TestServletContainerProvider.getServletNames().contains(servletName);
    }

    @GET
    @Path("immutableServletNames")
    public boolean isImmutableServletNames() {
        return TestServletContainerProvider.isImmutableServletNames();
    }

}
