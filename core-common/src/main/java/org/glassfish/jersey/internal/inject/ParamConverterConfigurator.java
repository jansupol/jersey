/*
 * Copyright (c) 2017, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 Payara Foundation and/or its affiliates.
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

package org.glassfish.jersey.internal.inject;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.glassfish.jersey.innate.inject.Bindings;
import org.glassfish.jersey.innate.inject.InjectionIds;
import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;

/**
 * Configurator which initializes and register {@link ParamConverters.AggregatedProvider} instances into {@link InjectionManager}.
 *
 * @author Petr Bouda
 */
public class ParamConverterConfigurator implements BootstrapConfigurator {

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        long id = injectionManager.getRuntimeType() == RuntimeType.CLIENT
                ? InjectionIds.CLIENT_AGGREGATED_PROVIDER.id()
                : InjectionIds.SERVER_AGGREGATED_PROVIDER.id();
        final Binding aggregatedConverters =
                Bindings.service(ParamConverters.AggregatedProvider.class)
                    .to(ParamConverterProvider.class).id(id);
        injectionManager.register(aggregatedConverters);
    }
}
