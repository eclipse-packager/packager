/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm.build;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.util.encoders.Hex;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.header.Header;

public class PayloadProcessors {

    private PayloadProcessors() {
    }

    /**
     * Create the payload size values for @{link {@link RpmTag#PAYLOAD_SIZE} and {@link RpmTag#PAYLOAD_SIZE_ALT}}.
     *
     * @return the payload processor
     */
    public static PayloadProcessor payloadSize() {
        return new PayloadProcessor() {
            private long payloadSize;

            private long archiveSize;

            @Override
            public void feedRawPayloadData(final ByteBuffer data) {
                payloadSize += data.remaining();
            }

            @Override
            public void feedCompressedPayloadData(final ByteBuffer data) {
                archiveSize += data.remaining();
            }

            @Override
            public void finish(final Header<RpmTag> header) {
                header.putLong(RpmTag.PAYLOAD_SIZE, payloadSize);
                header.putLong(RpmTag.PAYLOAD_SIZE_ALT, archiveSize);
            }
        };
    }

    /**
     * Create the payload digest values for @{link {@link RpmTag#PAYLOAD_DIGEST} and {@link RpmTag#PAYLOAD_DIGEST_ALT}}.
     *
     * @param algorithm The digest algorithm to use
     * @return The payload processor
     * @throws NoSuchAlgorithmException In case the algorithm isn't supported by the JVM
     */
    public static PayloadProcessor payloadDigest(final DigestAlgorithm algorithm) throws NoSuchAlgorithmException {
        final MessageDigest digestRaw = algorithm.createDigest();
        final MessageDigest digestCompressed = algorithm.createDigest();

        return new PayloadProcessor() {
            @Override
            public void feedRawPayloadData(final ByteBuffer data) {
                digestRaw.update(data);
            }

            @Override
            public void feedCompressedPayloadData(final ByteBuffer data) {
                digestCompressed.update(data);
            }

            @Override
            public void finish(final Header<RpmTag> header) {
                final String raw = Hex.toHexString(digestRaw.digest());
                final String compressed = Hex.toHexString(digestCompressed.digest());
                header.putStringArray(RpmTag.PAYLOAD_DIGEST, compressed);
                header.putStringArray(RpmTag.PAYLOAD_DIGEST_ALT, raw);
                header.putInt(RpmTag.PAYLOAD_DIGEST_ALGO, algorithm.getTag());
            }
        };
    }
}
