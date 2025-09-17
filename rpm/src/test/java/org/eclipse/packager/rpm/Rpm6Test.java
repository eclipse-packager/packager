/*
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.packager.rpm;

import org.eclipse.packager.rpm.app.Dumper;
import org.eclipse.packager.rpm.build.BuilderContext;
import org.eclipse.packager.rpm.build.BuilderOptions;
import org.eclipse.packager.rpm.build.RpmBuilder;
import org.eclipse.packager.rpm.build.RpmFileNameProvider;
import org.eclipse.packager.rpm.coding.PayloadCoding;
import org.eclipse.packager.rpm.coding.PayloadFlags;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.util.EnumSet.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.packager.rpm.RpmTag.FILE_SIZES;
import static org.eclipse.packager.rpm.RpmTag.LONG_FILE_SIZES;
import static org.eclipse.packager.rpm.RpmTag.RPM_FORMAT;

class Rpm6Test {
    private static final Path IN_BASE = Path.of("src", "test", "resources", "data", "in");

    @Test
    void testReadWriteRpm(final @TempDir Path outBase) throws IOException {
        final String name = "issue-24-test";
        final String version = "1.0.0";
        final String release = "1";
        final String architecture = "noarch";
        final String expectedRpmFileName = name + "-" + version + "-" + release + "." + architecture + ".rpm";
        final BuilderOptions options = new BuilderOptions();
        options.setFileNameProvider(RpmFileNameProvider.DEFAULT_FILENAME_PROVIDER);
        options.setRpmFormat(6);
        options.setPayloadCoding(PayloadCoding.ZSTD);
        options.setPayloadFlags(new PayloadFlags(PayloadCoding.ZSTD, 19));

        try (final RpmBuilder builder = new RpmBuilder(name, new RpmVersion(version, release), architecture, outBase, options)) {
            final Path outFile = builder.getTargetFile();
            final BuilderContext ctx = builder.newContext();

            ctx.addDirectory("/etc/test3"); // 1
            ctx.addDirectory("etc/test3/a"); // 2
            ctx.addDirectory("//etc/test3/b"); // 3
            ctx.addDirectory("/etc/"); // 4

            ctx.addDirectory("/var/lib/test3", finfo -> finfo.setUser("")); // 5

            ctx.addFile("/etc/test3/file1", IN_BASE.resolve("file1"), BuilderContext.pathProvider().customize(finfo -> finfo.setFileFlags(of(FileFlags.CONFIGURATION)))); // 6

            ctx.addFile("/etc/test3/file2", new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)), finfo -> {
                finfo.setTimestamp(LocalDateTime.of(2014, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
                finfo.setFileFlags(of(FileFlags.CONFIGURATION));
            }); // 7

            ctx.addSymbolicLink("/etc/test3/file3", "/etc/test3/file1"); // 8

            builder.build();
            final String rpmFileName = options.getFileNameProvider().getRpmFileName(builder.getName(), builder.getVersion(), builder.getArchitecture());
            assertThat(rpmFileName).isEqualTo(expectedRpmFileName);
            assertThat(outFile.getFileName()).hasToString(expectedRpmFileName);
        }

        try (final RpmInputStream in = new RpmInputStream(Files.newInputStream(outBase.resolve(expectedRpmFileName)))) {
            Dumper.dumpAll(in);
            final InputHeader<RpmTag> header = in.getPayloadHeader();
            final Integer rpmFormat = header.getInteger(RPM_FORMAT);
            assertThat(rpmFormat).isEqualTo(6);
            assertThat(header.getLongList(LONG_FILE_SIZES)).containsExactly(0L, 0L, 0L, 0L, 6L, 3L, 16L, 0L);
            assertThat( header.getIntegerList(FILE_SIZES)).isNull();
            assertThat(header.getLong(RpmTag.PAYLOAD_SIZE)).isEqualTo(1152L);
            assertThat(header.getLong(RpmTag.PAYLOAD_SIZE_ALT)).isGreaterThanOrEqualTo(184L); // XXX: compressed size varies
        }
    }
}
