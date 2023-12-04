package org.glassfish.jersey.inject.weld.internal.managed;

import org.glassfish.jersey.inject.weld.internal.injector.JerseyClientCreationalContext;

import java.util.UUID;

public final class ContextSafe {
    private static class ContextLockPair {
        private final JerseyClientCreationalContext context;
        private final String key;

        private ContextLockPair(JerseyClientCreationalContext context, String key) {
            this.context = context;
            this.key = key;
        }
    }
    private static final ThreadLocal<ContextLockPair> CURRENT_CONTEXT = new ThreadLocal<>();

    public static String lockContext(JerseyClientCreationalContext context) {
        UUID uuid = UUID.randomUUID();
        String key = uuid.toString();
        CURRENT_CONTEXT.set(new ContextLockPair(context, key));
        return key;
    }

    public static JerseyClientCreationalContext get() {
        return CURRENT_CONTEXT.get().context;
    }

    public static void unlock(String key) {
        ContextLockPair pair = CURRENT_CONTEXT.get();
        if (!pair.key.equals(key)) {
            throw new IllegalStateException("The CreationalContext safe has been compromised");
        }
        CURRENT_CONTEXT.remove();
    }
}
