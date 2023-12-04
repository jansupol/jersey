package org.glassfish.jersey.inject.weld;

import org.glassfish.jersey.internal.inject.Injections;
import org.junit.jupiter.api.BeforeEach;

import javax.ws.rs.RuntimeType;

public class TestClientParent extends TestParent {
    @BeforeEach
    public void init() {
        injectionManager = Injections.createInjectionManager(RuntimeType.CLIENT);
    }
}
