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

package org.eclipse.packager.rpm.signature;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.Rpms;
import org.eclipse.packager.rpm.header.Header;

public final class SignatureProcessors {
    private SignatureProcessors() {
    }

    public static SignatureProcessor size(final int rpmFormat) {
        return new SignatureProcessor() {

            private long headerSize;

            private long payloadSize;

            @Override
            public void feedHeader(final ByteBuffer header) {
                this.headerSize = header.remaining();
            }

            @Override
            public void feedPayloadData(final ByteBuffer data) {
                this.payloadSize += data.remaining();
            }

            @Override
            public void finish(final Header<RpmSignatureTag> signature) {
                signature.putSize(this.headerSize + this.payloadSize, RpmSignatureTag.SIZE, RpmSignatureTag.LONGSIZE, rpmFormat);
            }
        };
    }

    public static SignatureProcessor payloadSize(final int rpmFormat) {
        return new SignatureProcessor() {

            private long archiveSize;

            @Override
            public void init(final long archiveSize) {
                this.archiveSize = archiveSize;
            }

            @Override
            public void feedHeader(final ByteBuffer header) {
            }

            @Override
            public void feedPayloadData(final ByteBuffer data) {
            }

            @Override
            public void finish(final Header<RpmSignatureTag> signature) {
                signature.putSize(this.archiveSize, RpmSignatureTag.PAYLOAD_SIZE, RpmSignatureTag.LONGARCHIVESIZE, rpmFormat);
            }
        };
    }

    public static SignatureProcessor sha256Header() {
        return new SignatureProcessor() {

            private String value;

            @Override
            public void feedHeader(final ByteBuffer header) {
                try {
                    final MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(header.slice());
                    this.value = Rpms.toHex(md.digest()).toLowerCase();
                } catch (final NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void feedPayloadData(final ByteBuffer data) {
                // we only work with the header
            }

            @Override
            public void finish(final Header<RpmSignatureTag> signature) {
                signature.putString(RpmSignatureTag.SHA256HEADER, this.value);
            }
        };
    }

    public static SignatureProcessor sha1Header() {
        return new SignatureProcessor() {

            private String value;

            @Override
            public void feedHeader(final ByteBuffer header) {
                try {
                    final MessageDigest md = MessageDigest.getInstance("SHA1");
                    md.update(header.slice());
                    this.value = Rpms.toHex(md.digest()).toLowerCase();
                } catch (final NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void feedPayloadData(final ByteBuffer data) {
                // we only work with the header
            }

            @Override
            public void finish(final Header<RpmSignatureTag> signature) {
                signature.putString(RpmSignatureTag.SHA1HEADER, this.value);
            }
        };
    }

    public static SignatureProcessor md5() {
        return new SignatureProcessor() {

            private MessageDigest digest;

            @Override
            public void init(final long archiveSize) {
                try {
                    this.digest = MessageDigest.getInstance("MD5");
                } catch (final NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void feedHeader(final ByteBuffer header) {
                this.digest.update(header);
            }

            @Override
            public void feedPayloadData(final ByteBuffer data) {
                this.digest.update(data);
            }

            @Override
            public void finish(final Header<RpmSignatureTag> signature) {
                signature.putBlob(RpmSignatureTag.MD5, this.digest.digest());
            }
        };
    }
}
