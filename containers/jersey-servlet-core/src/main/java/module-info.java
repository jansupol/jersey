/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.servlet.internal.spi.ExtendedServletContainerProvider;
import org.glassfish.jersey.servlet.internal.spi.RequestContextProvider;
import org.glassfish.jersey.servlet.internal.spi.RequestScopedInitializerProvider;
import org.glassfish.jersey.servlet.internal.spi.ServletContainerProvider;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegate;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegateProvider;
import org.glassfish.jersey.servlet.spi.FilterUrlMappingsProvider;

module org.glassfish.jersey.container.servlet.core {
    requires java.logging;
    requires java.naming;

    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires jakarta.persistence;
    requires static jakarta.servlet;

    requires org.glassfish.jersey.core.common;
    requires org.glassfish.jersey.core.server;

    exports org.glassfish.jersey.servlet;
    exports org.glassfish.jersey.servlet.internal;
    exports org.glassfish.jersey.servlet.internal.spi;
    exports org.glassfish.jersey.servlet.spi;

    opens org.glassfish.jersey.servlet;

    uses AsyncContextDelegate;
    uses AsyncContextDelegateProvider;
    uses FilterUrlMappingsProvider;

    uses ExtendedServletContainerProvider;
    uses RequestContextProvider;
    uses RequestScopedInitializerProvider;
    uses ServletContainerProvider;
}