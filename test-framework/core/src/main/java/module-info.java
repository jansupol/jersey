/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates. All rights reserved.
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

module org.glassfish.jersey.tests.framework.core {
    requires jakarta.ws.rs;
    requires jakarta.xml.bind;
    requires static jakarta.servlet;
    requires static jakarta.persistence;

    requires java.logging;

    requires org.glassfish.jersey.core.common;
    requires org.glassfish.jersey.core.client;
    requires org.glassfish.jersey.core.server;
    requires org.glassfish.jersey.container.servlet.core;


    requires static junit;
    requires static org.junit.jupiter.api;

    requires static testng;

    exports org.glassfish.jersey.test;
    exports org.glassfish.jersey.test.spi;
    opens org.glassfish.jersey.test;
    opens org.glassfish.jersey.test.spi;
}