/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.servlet_25_config_reload;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author Jakub Podlesak
 */
@Path("reload")
public class ReloadResource {

    @GET
    @Produces("text/plain")
    public String get() {
        try {
            ReloadContainerLifecycleListener.getContainer().reload(
                    new ResourceConfig(HelloWorldResource.class,
                            ReloadResource.class,
                            AnotherResource.class,
                            ReloadContainerLifecycleListener.class)
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return "Reload resource";
    }
}
