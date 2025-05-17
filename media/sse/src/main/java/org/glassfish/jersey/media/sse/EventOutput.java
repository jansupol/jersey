/*
 * Copyright (c) 2012, 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.sse;

import org.glassfish.jersey.server.ChunkedOutput;

import java.nio.charset.StandardCharsets;

/**
 * Outbound Server-Sent Events channel.
 *
 * When returned from resource method, underlying connection is kept open and application
 * is able to send events. One instance of this class corresponds with exactly one HTTP connection.
 *
 * @author Pavel Bucek
 * @author Marek Potociar
 */
public class EventOutput extends ChunkedOutput<OutboundEvent> {
    // encoding does not matter for lower ASCII characters
    private static final byte[] SSE_EVENT_DELIMITER = "\n".getBytes(StandardCharsets.UTF_8);

    /**
     * Create new outbound Server-Sent Events channel.
     */
    public EventOutput() {
        super(SSE_EVENT_DELIMITER);
    }
}
