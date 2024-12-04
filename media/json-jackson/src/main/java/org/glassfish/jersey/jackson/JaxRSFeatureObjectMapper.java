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

package org.glassfish.jersey.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.jackson.internal.AbstractObjectMapper;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.JaxRSFeature;


/**
 * The Jackson {@link ObjectMapper} supporting {@link JaxRSFeature}s.
 */
public class JaxRSFeatureObjectMapper extends AbstractObjectMapper {

    public JaxRSFeatureObjectMapper() {
        super();
    }

    /**
     * Method for changing state of an on/off {@link org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.JaxRSFeature}
     * features.
     */
    public ObjectMapper configure(JaxRSFeature f, boolean state) {
        jaxrsFeatureBag.jaxrsFeature(f, state);
        return this;
    }

    /**
     * Method for enabling specified {@link org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.JaxRSFeature}s
     * for parser instances this object mapper creates.
     */
    public ObjectMapper enable(JaxRSFeature... features) {
        if (features != null) {
            for (JaxRSFeature f : features) {
                jaxrsFeatureBag.jaxrsFeature(f, true);
            }
        }
        return this;
    }

    /**
     * Method for disabling specified {@link org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.JaxRSFeature}s
     * for parser instances this object mapper creates.
     */
    public ObjectMapper disable(JaxRSFeature... features) {
        if (features != null) {
            for (JaxRSFeature f : features) {
                jaxrsFeatureBag.jaxrsFeature(f, false);
            }
        }
        return this;
    }
}
