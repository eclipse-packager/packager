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
 ********************************************************************************/
package org.eclipse.packager.rpm.signature;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.eclipse.packager.rpm.HashAlgorithm;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Header;
import org.eclipse.packager.rpm.header.Headers;
import org.eclipse.packager.rpm.info.RpmInformation;
import org.eclipse.packager.rpm.info.RpmInformations;
import org.eclipse.packager.rpm.parse.RpmInputStream;

/**
 * Sign existing RPM file by calling
 * {@link #perform(Path, InputStream, String, OutputStream, HashAlgorithm, int)}
 */
public class RpmFileSignatureProcessor {
    private RpmFileSignatureProcessor() {
        // Hide default constructor because of the static context
    }

    /**
     * <p>
     * Perform the signature of the given RPM file with the given private key. This
     * support only PGP. Write the result into the given {@link OutputStream}
     * </p>
     *
     * @param rpm the RPM file
     * @param privateKeyIn the encrypted private key as {@link InputStream}
     * @param passphrase the passphrase to decrypt the private key
     * @param out the {@link OutputStream} to write to
     * @param hashAlgorithm the hash algorithm
     * @param rpmFormat the RPM format
     * @throws PGPException if the private key cannot be extracted
     * @throws IOException if error happened with InputStream
     */
    public static void perform(final Path rpm, final InputStream privateKeyIn, final String passphrase, final OutputStream out, final HashAlgorithm hashAlgorithm, final int rpmFormat)
        throws  IOException, PGPException {

        final long leadLength = 96;
        final long signatureHeaderStart;
        final long signatureHeaderLength;
        final long payloadHeaderStart;
        final long payloadHeaderLength;
        final long payloadStart;
        final long archiveSize;
        final long payloadSize;
        final byte[] signatureHeader;

        if (!Files.exists(rpm)) {
            throw new IOException("The file " + rpm.getFileName() + " does not exist");
        }

        // Extract private key
        final PGPPrivateKey privateKey = getPrivateKey(privateKeyIn, passphrase);

        // Get the information of the RPM
        try (final RpmInputStream rpmIn = new RpmInputStream(new BufferedInputStream(Files.newInputStream(rpm)))) {
            signatureHeaderStart = rpmIn.getSignatureHeader().getStart();
            signatureHeaderLength = rpmIn.getSignatureHeader().getLength();
            payloadHeaderStart = rpmIn.getPayloadHeader().getStart();
            payloadHeaderLength = rpmIn.getPayloadHeader().getLength();
            final RpmInformation info = RpmInformations.makeInformation(rpmIn);
            payloadStart = info.getHeaderEnd();
            archiveSize = info.getArchiveSize();
        }

        if (signatureHeaderStart == 0L || signatureHeaderLength == 0L || payloadHeaderStart == 0L
            || payloadHeaderLength == 0L || payloadStart == 0L || archiveSize == 0L) {
            throw new IOException("Unable to read " + rpm.getFileName() + " informations.");
        }

        // Build the signature header by digest payload header + payload
        try (final FileChannel channelIn = FileChannel.open(rpm)) {
            payloadSize = channelIn.size() - payloadStart;
            channelIn.position(leadLength + signatureHeaderLength);
            final ByteBuffer payloadHeaderBuff = ByteBuffer.allocate((int) payloadHeaderLength);
            IOUtils.readFully(channelIn, payloadHeaderBuff);
            final ByteBuffer payloadBuff = ByteBuffer.allocate((int) payloadSize);
            IOUtils.readFully(channelIn, payloadBuff);
            signatureHeader = getSignature(privateKey, payloadHeaderBuff, payloadBuff, archiveSize, hashAlgorithm, rpmFormat);
        }

        // Write to the OutputStream
        try (final InputStream in = Files.newInputStream(rpm)) {
            IOUtils.copyLarge(in, out, 0, leadLength);
            IOUtils.skip(in, signatureHeaderLength);
            out.write(signatureHeader);
            IOUtils.copy(in, out);
        }
    }

    /**
     * <p>
     * Sign the payload with its header with the given private key, see <a href=
     * "https://rpm-software-management.github.io/rpm/manual/format.html">https://rpm-software-management.github.io/rpm/manual/format.html</a>
     * </p>
     *
     * @param privateKey the private key already extracted
     * @param payloadHeader the Payload's header as {@link ByteBuffer}
     * @param payload the Payload as {@link ByteBuffer}
     * @param archiveSize the archiveSize retrieved in {@link RpmInformation}
     * @param hashAlgorithm the hash algorithm
     * @param rpmFormat the RPM format
     * @return the signature header as a bytes array
     * @throws IOException if an error occurs while writing the signature
     */
    private static byte[] getSignature(final PGPPrivateKey privateKey, final ByteBuffer payloadHeader, final ByteBuffer payload,
                                       final long archiveSize, final HashAlgorithm hashAlgorithm, final int rpmFormat) throws IOException {
        final Header<RpmSignatureTag> signatureHeader = new Header<>();
        final List<SignatureProcessor> signatureProcessors = getSignatureProcessors(privateKey, hashAlgorithm, rpmFormat);
        payloadHeader.flip();
        payload.flip();
        for (final SignatureProcessor processor : signatureProcessors) {
            processor.init(archiveSize);
            processor.feedHeader(payloadHeader.slice());
            processor.feedPayloadData(payload.slice());
            processor.finish(signatureHeader);
        }
        final ByteBuffer signatureBuf = Headers.render(signatureHeader.makeEntries(), true, Rpms.IMMUTABLE_TAG_SIGNATURE);
        final int payloadSize = signatureBuf.remaining();
        final int padding = Rpms.padding(payloadSize);
        final byte[] signature = safeReadBuffer(signatureBuf);
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(signature);
        if (padding > 0) {
            result.write(safeReadBuffer(ByteBuffer.wrap(Rpms.EMPTY_128, 0, padding)));
        }
        return result.toByteArray();
    }

    /**
     * <p>
     * Safe read (without buffer bytes) the given buffer and return it as a byte
     * array
     * </p>
     *
     * @param buf the {@link ByteBuffer} to read
     * @return a bytes array
     */
    private static byte[] safeReadBuffer(final ByteBuffer buf) {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (buf.hasRemaining()) {
            result.write(buf.get());
        }
        return result.toByteArray();
    }

    /**
     * <p>
     * Return all {@link SignatureProcessor} required to perform signature
     * {@link SignatureProcessors}
     * </p>
     *
     * @param privateKey the private key, already extracted
     * @param hashAlgorithm the hash algorithm
     * @param rpmFormat the RPM format
     * @return {@link List<SignatureProcessor>} of {@link SignatureProcessor}
     */
    private static List<SignatureProcessor> getSignatureProcessors(final PGPPrivateKey privateKey, final HashAlgorithm hashAlgorithm, final int rpmFormat) {
        final List<SignatureProcessor> signatureProcessors = new ArrayList<>();
        signatureProcessors.add(SignatureProcessors.size(rpmFormat));
        signatureProcessors.add(SignatureProcessors.sha256Header());
        signatureProcessors.add(SignatureProcessors.sha1Header());
        signatureProcessors.add(SignatureProcessors.md5());
        signatureProcessors.add(SignatureProcessors.payloadSize(rpmFormat));
        signatureProcessors.add(new RsaSignatureProcessor(privateKey, hashAlgorithm));
        return signatureProcessors;
    }

    /**
     * <p>
     * Decrypt and retrieve the private key
     * </p>
     *
     * @param privateKeyIn InputStream containing the encrypted private key
     * @param passphrase passphrase to decrypt private key
     * @return private key as {@link PGPPrivateKey}
     * @throws PGPException if the private key cannot be extracted
     * @throws IOException if error happened with InputStream
     */
    private static PGPPrivateKey getPrivateKey(final InputStream privateKeyIn, final String passphrase)
        throws PGPException, IOException {
        final ArmoredInputStream armor = new ArmoredInputStream(privateKeyIn);
        final PGPSecretKeyRing secretKeyRing = new BcPGPSecretKeyRing(armor);
        final PGPSecretKey secretKey = secretKeyRing.getSecretKey();
        return secretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
            .build(passphrase.toCharArray()));
    }
}
