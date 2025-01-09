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

package org.glassfish.jersey.jackson.internal;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.base.ProviderBase;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.JaxRSFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Internal holder class for {@link JaxRSFeature} settings and their values.
 */
public class JaxrsFeatureBag<T extends JaxrsFeatureBag> {
    protected static final String JAXRS_FEATURE = "jersey.config.jackson.jaxrs.feature";

    private static class JaxRSFeatureState {
        /* package */ final JaxRSFeature feature;
        /* package */ final boolean state;
        public JaxRSFeatureState(JaxRSFeature feature, boolean state) {
            this.feature = feature;
            this.state = state;
        }
    }

    private Optional<List<JaxRSFeatureState>> jaxRSFeature = Optional.empty();

    public T jaxrsFeature(JaxRSFeature feature, boolean state) {
        if (!jaxRSFeature.isPresent()) {
            jaxRSFeature = Optional.of(new ArrayList<>());
        }
        jaxRSFeature.ifPresent(list -> list.add(new JaxrsFeatureBag.JaxRSFeatureState(feature, state)));
        return (T) this;
    }

    protected boolean hasJaxrsFeature() {
        return jaxRSFeature.isPresent();
    }

    /* package */ void configureJaxrsFeatures(ProviderBase providerBase) {
        jaxRSFeature.ifPresent(list -> list.stream().forEach(state -> providerBase.configure(state.feature, state.state)));
    }
}
