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

package org.glassfish.jersey.netty.connector;

import org.glassfish.jersey.client.ClientRequest;

import java.net.URI;

/**
 * Adjustable connection pooling controller.
 */
public class NettyConnectionController {
    /**
     * Get the group of connections to be pooled, purged idle, and reused together.
     *
     * @param clientRequest the HTTP client request.
     * @param uri the uri for the HTTP client request.
     * @param hostName the hostname for the request. Can differ from the hostname in the uri based on other request attributes.
     * @param port the real port for the request. Can differ from the port in the uri based on other request attributes.
     * @return the group of connections identifier.
     */
    public String getConnectionGroup(ClientRequest clientRequest, URI uri, String hostName, int port) {
        return uri.getScheme() + "://" + hostName + ":" + port;
    }
}
