package org.glassfish.jersey.inject.weld.binder.common;

import org.glassfish.jersey.inject.weld.TestParent;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.spi.HeaderDelegateProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.RuntimeType;
import java.util.List;

public class CoreCommonClientBindingsTest extends TestParent {
    static {
        TestParent.runtimeType = RuntimeType.CLIENT;
    }

    @Test
    public void testProviders() {
        injectionManager.completeRegistration();
        injectionManager.register(new MessagingBinders.HeaderDelegateProviders());
        List<HeaderDelegateProvider> ncp = injectionManager.getAllInstances(HeaderDelegateProvider.class);
        Assertions.assertEquals(10, ncp.size());
    }
}
