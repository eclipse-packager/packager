/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   mat1e, Groupe EDF - initial API and implementation
 *   Jens Reimann, Red Hat, Inc
 ********************************************************************************/
package org.eclipse.packager.rpm.signature;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.packager.rpm.RpmSignatureTag.MD5;
import static org.eclipse.packager.rpm.RpmSignatureTag.PAYLOAD_SIZE;
import static org.eclipse.packager.rpm.RpmSignatureTag.PGP;
import static org.eclipse.packager.rpm.RpmSignatureTag.SHA1HEADER;
import static org.eclipse.packager.rpm.RpmSignatureTag.SIZE;
import static org.testcontainers.containers.Container.ExecResult;
import static org.testcontainers.images.builder.Transferable.DEFAULT_FILE_MODE;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.packager.rpm.HashAlgorithm;
import org.eclipse.packager.rpm.RpmFormat;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RpmFileSignatureProcessorTest {
    private static final String PASSPHRASE = "testkey"; // Do not change

    private static final Path RPM = Path.of("src/test/resources/data/org.eclipse.scada-0.2.1-1.noarch.rpm");

    private static final Path PRIVATE_KEY = Path.of("src/test/resources/key/private_key.txt");

    private static final Path PUBLIC_KEY = Path.of("src/test/resources/key/public_key.txt");

    private static final String IMAGE_NAME = "registry.access.redhat.com/ubi9/ubi-minimal:latest";

    private static final String COMMAND = "sleep infinity";

    static Path signedRpm;

    @TempDir
    static Path resultDirectory;

    @BeforeAll
    static void testSigningExistingRpm() throws IOException, PGPException {
        assertThat(RPM).exists();
        assertThat(PRIVATE_KEY).exists();

        // Init the signed RPM
        signedRpm = resultDirectory.resolve("org.eclipse.scada-0.2.1-1.noarch.rpm");

        try (final OutputStream resultOut = Files.newOutputStream(signedRpm, CREATE_NEW); final InputStream privateKeyStream = Files.newInputStream(PRIVATE_KEY)) {
            // Sign the RPM
            RpmFileSignatureProcessor.perform(RPM, privateKeyStream, PASSPHRASE, resultOut, HashAlgorithm.SHA256, RpmFormat.RPM_4.getFormat());

            // Read the signed rpm file
            try (final RpmInputStream initialRpm = new RpmInputStream(new BufferedInputStream(Files.newInputStream(RPM))); final RpmInputStream rpmSigned = new RpmInputStream(new BufferedInputStream(Files.newInputStream(signedRpm)))) {
                // Get information of the initial rpm file
                final InputHeader<RpmSignatureTag> initialHeader = initialRpm.getSignatureHeader();
                final int initialSize = initialHeader.getInteger(SIZE);
                final int initialPayloadSize = initialHeader.getInteger(PAYLOAD_SIZE);
                final String initialSha1 = initialHeader.getString(SHA1HEADER);
                final byte[] initialMd5 = initialHeader.getByteArray(MD5);

                // Get information of the signed rpm file
                final InputHeader<RpmSignatureTag> signedHeader = rpmSigned.getSignatureHeader();
                final int signedSize = signedHeader.getInteger(SIZE);
                final int signedPayloadSize = signedHeader.getInteger(PAYLOAD_SIZE);
                final String signedSha1 = signedHeader.getString(SHA1HEADER);
                final byte[] signedMd5 = signedHeader.getByteArray(MD5);
                final byte[] pgpSignature = signedHeader.getByteArray(PGP);

                // Compare information values of initial rpm and signed rpm
                assertThat(signedSize).isEqualTo(initialSize);
                assertThat(signedPayloadSize).isEqualTo(initialPayloadSize);
                assertThat(signedSha1).isEqualTo(initialSha1);
                assertThat(signedMd5).isEqualTo(initialMd5);

                // Verify if signature is present
                assertThat(pgpSignature).isNotNull();
            }
        }
    }

    @Test
    void verifyRpmSignature() throws Exception {
        assertThat(PUBLIC_KEY).exists();
        assertThat(signedRpm).exists();

        // extract the plain file name
        final Path publicKeyName = PUBLIC_KEY.getFileName();
        final Path rpmFileName = signedRpm.getFileName();

        try (final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))) {
            container.setCommand(COMMAND);
            container.withCopyToContainer(Transferable.of(Files.readAllBytes(PUBLIC_KEY), DEFAULT_FILE_MODE), "/" + publicKeyName);
            container.withCopyToContainer(Transferable.of(Files.readAllBytes(signedRpm), DEFAULT_FILE_MODE), "/" + rpmFileName);
            container.start();
            final ExecResult importResult = container.execInContainer("rpm", "--import", "/" + publicKeyName);
            assertThat(importResult.getExitCode()).isZero();
            final ExecResult checksigResult = container.execInContainer("rpm", "--verbose", "--checksig", "/" + rpmFileName);
            assertThat(checksigResult.getExitCode()).isZero();
            final String stdout = checksigResult.getStdout();
            assertThat(stdout).containsIgnoringWhitespaces("V4 RSA/SHA256 Signature, key ID 679f5723: OK");
        }
    }
}
