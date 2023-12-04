package org.glassfish.jersey.inject.weld.internal.managed;

import org.glassfish.jersey.inject.weld.internal.inject.InitialializableListBinding;
import org.glassfish.jersey.inject.weld.internal.inject.InitializableInstanceBinding;
import org.glassfish.jersey.internal.inject.InstanceBinding;

import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

class ProvidersBinders {
    private final Map<Type, InitialializableListBinding<?>> userBindings = new HashMap<>();

    boolean init(InstanceBinding<?> userBinding) {
        boolean init = false;
        for (Type contract : userBinding.getContracts()) {
            InitialializableListBinding<?> initializableBinding = userBindings.get(contract);
            if (initializableBinding != null) {
                init = true;
                initializableBinding.init(userBinding);
            }
        }
    }
}
