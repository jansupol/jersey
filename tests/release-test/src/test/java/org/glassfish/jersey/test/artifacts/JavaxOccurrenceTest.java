/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.artifacts;

import org.glassfish.jersey.message.internal.ReaderWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class JavaxOccurrenceTest {

    private static final String[] packages = {"javax.ws.rs"};

    @Test
    public void testSources() throws IOException {
        TestResult result = new TestResult();
        Path root = Paths.get(".").toAbsolutePath().getParent().getParent().getParent();
        Assert.assertTrue(Files.exists(root) && Files.isDirectory(root));
        Files.walkFileTree(root, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isSourceFolder(dir)) {
                    result.ok().append("parsing ").println(dir.toString());
                    Files.walkFileTree(dir.resolve("main"), new SrcWalker(result));
                    return FileVisitResult.CONTINUE;
                } else if (isModule(dir)) {
                    return FileVisitResult.CONTINUE;
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        if (result.exception().builder.length() == 0) {
            result.ok().append("All java files are Jakartified correctly");
        }
        if (!result.result()) {
            Assert.fail();
        }
    }

    private static class SrcWalker implements FileVisitor<Path> {

        private final TestResult testResult;

        private SrcWalker(TestResult testResult) {
            this.testResult = testResult;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String path = dir.toAbsolutePath().toString();
            boolean cont = (path.contains("main") || path.contains("test")) && !path.contains("resources");
            return cont ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(".java")) {
                parseFile(file, testResult);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }

    private static boolean isModule(Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return stream.anyMatch(path1 -> path1.getFileName().toString().equals("pom.xml"));
        }
    }

    private static boolean isSourceFolder(Path path) {
        return path.getFileName().startsWith("src");
    }

    private static void parseFile(Path path, TestResult testResult) throws IOException {
        String file = path.toString();
        if (file.contains("MetricsRequestEventListener") || file.contains("ObservationRequestEventListener")) {
            // these contain both javax && jakarta
            return;
        }
        for (String row : ReaderWriter.readFromAsString(Files.newBufferedReader(path)).split("\n")) {
            parseRow(file, row, testResult);
        }
    }

    private static void parseRow(String path, String row, TestResult result) {
        for (String pkg : packages) {
            if (row.contains(pkg)) {
                result.exception().append("Error in file ").append(path).append(" - contains ").println(pkg);
            }
        }
    }
}
