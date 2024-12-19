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

package org.glassfish.jersey.media.multipart;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tests in clientFilter before the multipart provider is invoked.
 * Check the workers are set.
 *
 * Modified MP Rest Client TCK tests
 */
public class ClientFilterTests {
    /**
     * Tests that a single file is upload. The response is a simple JSON response with the file information.
     *
     * @throws Exception
     *             if a test error occurs
     */
    @Test
    public void uploadFile() throws Exception {
        try (Client client = createClient()) {
            final byte[] content;
            try (InputStream in = ClientFilterTests.class.getResourceAsStream("/multipart/test-file1.txt")) {
                Assertions.assertNotNull(in, "Could not find /multipart/test-file1.txt");
                content = in.readAllBytes();
            }
            // Send in an InputStream to ensure it works with an InputStream
            final List<EntityPart> files = List.of(EntityPart.withFileName("test-file1.txt")
                    .content(new ByteArrayInputStream(content))
                    .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .build());
            try (Response response = client.target("http://localhost").request()
                    .post(Entity.entity(files, MediaType.MULTIPART_FORM_DATA))) {
                Assertions.assertEquals(201, response.getStatus());
                final JsonArray jsonArray = response.readEntity(JsonArray.class);
                Assertions.assertNotNull(jsonArray);
                Assertions.assertEquals(jsonArray.size(), 1);
                final JsonObject json = jsonArray.getJsonObject(0);
                Assertions.assertEquals(json.getString("name"), "test-file1.txt");
                Assertions.assertEquals(json.getString("fileName"), "test-file1.txt");
                Assertions.assertEquals(json.getString("content"), "This is a test file for file 1.");
            }
        }
    }

    /**
     * Tests that two files are upload. The response is a simple JSON response with the file information.
     *
     * @throws Exception
     *             if a test error occurs
     */
    @Test
    public void uploadMultipleFiles() throws Exception {
        try (Client client = createClient()) {
            final Map<String, byte[]> entityPartContent = new LinkedHashMap<>(2);
            try (InputStream in = ClientFilterTests.class.getResourceAsStream("/multipart/test-file1.txt")) {
                Assertions.assertNotNull(in, "Could not find /multipart/test-file1.txt");
                entityPartContent.put("test-file1.txt", in.readAllBytes());
            }
            try (InputStream in = ClientFilterTests.class.getResourceAsStream("/multipart/test-file2.txt")) {
                Assertions.assertNotNull(in, "Could not find /multipart/test-file2.txt");
                entityPartContent.put("test-file2.txt", in.readAllBytes());
            }
            final List<EntityPart> files = entityPartContent.entrySet()
                    .stream()
                    .map((entry) -> {
                        try {
                            return EntityPart.withName(entry.getKey())
                                    .fileName(entry.getKey())
                                    .content(entry.getValue())
                                    .mediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                                    .build();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .collect(Collectors.toList());

            try (Response response = client.target("http://localhost").request()
                    .post(Entity.entity(files, MediaType.MULTIPART_FORM_DATA))) {
                Assertions.assertEquals(201, response.getStatus());
                final JsonArray jsonArray = response.readEntity(JsonArray.class);
                Assertions.assertNotNull(jsonArray);
                Assertions.assertEquals(jsonArray.size(), 2);
                // Don't assume the results are in a specific order
                for (JsonValue value : jsonArray) {
                    final JsonObject json = value.asJsonObject();
                    if (json.getString("name").equals("test-file1.txt")) {
                        Assertions.assertEquals(json.getString("fileName"), "test-file1.txt");
                        Assertions.assertEquals(json.getString("content"), "This is a test file for file 1.");
                    } else if (json.getString("name").equals("test-file2.txt")) {
                        Assertions.assertEquals(json.getString("fileName"), "test-file2.txt");
                        Assertions.assertEquals(json.getString("content"), "This is a test file for file 2.");
                    } else {
                        Assertions.fail(String.format("Unexpected entry %s in JSON response: %n%s", json, jsonArray));
                    }
                }
            }
        }
    }

    private static Client createClient() {
        return ClientBuilder.newClient().register(new FileManagerFilter());
    }

    public static class FileManagerFilter implements ClientRequestFilter {

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException {
            if (requestContext.getMethod().equals("POST")) {
                // Download the file
                @SuppressWarnings("unchecked")
                final List<EntityPart> entityParts = (List<EntityPart>) requestContext.getEntity();
                final JsonArrayBuilder jsonBuilder = Json.createArrayBuilder();
                for (EntityPart part : entityParts) {
                    final JsonObjectBuilder jsonPartBuilder = Json.createObjectBuilder();
                    jsonPartBuilder.add("name", part.getName());
                    if (part.getFileName().isPresent()) {
                        jsonPartBuilder.add("fileName", part.getFileName().get());
                    } else {
                        throw new BadRequestException("No file name for entity part " + part);
                    }
                    jsonPartBuilder.add("content", part.getContent(String.class));
                    jsonBuilder.add(jsonPartBuilder);
                }
                requestContext.abortWith(Response.status(201).entity(jsonBuilder.build()).build());
            } else {
                requestContext
                        .abortWith(Response.status(Response.Status.BAD_REQUEST).entity("Invalid request").build());
            }
        }
    }
}
