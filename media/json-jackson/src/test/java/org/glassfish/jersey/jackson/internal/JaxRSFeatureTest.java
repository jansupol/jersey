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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.JaxRSFeatureObjectMapper;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.JaxRSFeature;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class JaxRSFeatureTest {
    @Test
    public void testJaxrsFeatureOnJacksonFeature() {
        Client client = ClientBuilder.newClient()
                .register(new JacksonFeature().jaxrsFeature(JaxRSFeature.READ_FULL_STREAM, false))
                .register(JaxrsFeatureFilter.class);

        try (Response r = client.target("http://xxx.yyy").request().get()) {
            MatcherAssert.assertThat(r.getStatus(), Matchers.is(200));
        }
    }

    @Test
    public void testJaxrsFeatureOnContextResolver() {
        Client client = ClientBuilder.newClient()
                .register(JacksonFeature.class)
                .register(JaxrsFetureContextResolver.class)
                .register(JaxrsFeatureFilter.class);

        try (Response r = client.target("http://xxx.yyy").request().get()) {
            MatcherAssert.assertThat(r.getStatus(), Matchers.is(200));
        }
    }


    public static class JaxrsFeatureFilter implements ClientRequestFilter {
        private final DefaultJacksonJaxbJsonProvider jacksonProvider;
        @Inject
        public JaxrsFeatureFilter(Providers allProviders) {
            jacksonProvider = (DefaultJacksonJaxbJsonProvider)
                    allProviders.getMessageBodyReader(Object.class, Object.class, null, MediaType.APPLICATION_JSON_TYPE);
            try {
                jacksonProvider.readFrom(Object.class, Object.class, null, MediaType.APPLICATION_JSON_TYPE, null,
                        new ByteArrayInputStream("{}".getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            Response.Status status = jacksonProvider.isEnabled(JaxRSFeature.READ_FULL_STREAM)
                    ? Response.Status.FORBIDDEN
                    : Response.Status.OK;
            requestContext.abortWith(Response.status(status).build());
        }
    }

    public static class JaxrsFetureContextResolver implements ContextResolver<ObjectMapper> {

        @Override
        public ObjectMapper getContext(Class<?> type) {
            JaxRSFeatureObjectMapper objectMapper = new JaxRSFeatureObjectMapper();
            objectMapper.disable(JaxRSFeature.READ_FULL_STREAM);
            return objectMapper;
        }
    }
}
