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

package org.glassfish.jersey.innate;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.model.internal.CommonConfig;
import org.glassfish.jersey.model.internal.ComponentBag;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.FeatureContext;
import java.util.Map;

/**
 * Feature Context to be used during the Bootstrap Preinitialization phase
 */
class PreinitializationFeatureContext implements FeatureContext {

    private final AbstractBinder binder;

    PreinitializationFeatureContext() {
        this.binder = new AbstractBinder() {
            @Override
            protected void configure() {

            }
        };
    }

    @Override
    public Configuration getConfiguration() {
        return new CommonConfig(RuntimeType.SERVER, ComponentBag.INCLUDE_ALL);
    }

    @Override
    public FeatureContext property(String name, Object value) {
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass) {
        binder.bindAsContract(componentClass);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, int priority) {
        binder.bindAsContract(componentClass).ranked(priority);
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Class<?>... contracts) {
        final ClassBinding binding = binder.bind(componentClass);
        if (contracts != null) {
            for (Class<?> contract : contracts) {
                binding.to(contract);
            }
        }
        return this;
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        for (Map.Entry<Class<?>, Integer> contract : contracts.entrySet()) {
            final AbstractBinder abstractBinder = new AbstractBinder() {
                @Override
                protected void configure() {
                }
            };
            final ClassBinding binding = abstractBinder.bind(componentClass);
            binding.to(contract.getKey()).ranked(contract.getValue());
            binder.install(abstractBinder);
        }
        return this;
    }

    @Override
    public FeatureContext register(Object component) {
        if (InjectionManager.class.isInstance(component)) {
            ((InjectionManager) component).register(binder);
        } else if (AbstractBinder.class.isInstance(component)) {
            binder.install((AbstractBinder) component);
        } else {
            binder.bind(component).to(component.getClass());
        }
        return this;
    }

    @Override
    public FeatureContext register(Object component, int priority) {
        binder.bind(component).to(component.getClass()).ranked(priority);
        return this;
    }

    @Override
    public FeatureContext register(Object component, Class<?>... contracts) {
        Binding binding = binder.bind(component);
        if (contracts != null) {
            for (Class<?> contract : contracts) {
                binding.to(contract);
            }
        }
        return this;
    }

    @Override
    public FeatureContext register(Object component, Map<Class<?>, Integer> contracts) {
        for (Map.Entry<Class<?>, Integer> contract : contracts.entrySet()) {
            final AbstractBinder abstractBinder = new AbstractBinder() {
                @Override
                protected void configure() {
                }
            };
            final Binding binding = abstractBinder.bind(component);
            binding.to(contract.getKey()).ranked(contract.getValue());
            binder.install(abstractBinder);
        }
        return this;
    }
}
