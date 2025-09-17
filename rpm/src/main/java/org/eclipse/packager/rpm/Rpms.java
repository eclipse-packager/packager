/*
 * Copyright (c) 2015, 2019 Contributors to the Eclipse Foundation
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

import org.eclipse.packager.rpm.parse.HeaderValue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

public class Rpms {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    public static final byte[] LEAD_MAGIC = new byte[] { (byte) 0xED, (byte) 0xAB, (byte) 0xEE, (byte) 0xDB };

    public static final byte[] HEADER_MAGIC = new byte[] { (byte) 0x8E, (byte) 0xAD, (byte) 0xE8 };

    public static final byte[] EMPTY_128;

    public static final int IMMUTABLE_TAG_SIGNATURE = RpmTag.HEADER_SIGNATURES.getValue();

    public static final int IMMUTABLE_TAG_HEADER = RpmTag.HEADER_IMMUTABLE.getValue();

    static {
        EMPTY_128 = new byte[128];
        Arrays.fill(EMPTY_128, (byte) 0);
    }

    private Rpms() {}

    public static String toHex(final byte[] data) {
        return toHex(data, Integer.MAX_VALUE);
    }

    public static String toHex(final byte[] data, final int maxWidth) {
        return toHex(data, 0, data.length, maxWidth);
    }

    public static String toHex(final byte[] data, final int offset, final int length, final int maxWidth) {
        final StringBuilder sb = new StringBuilder(length * 2); // not considering line breaks

        int lc = 0;
        for (int i = 0; i < length; i++) {
            if (maxWidth > 0 && lc >= maxWidth) {
                sb.append(System.lineSeparator());
                lc = 0;
            }

            final int b = data[offset + i] & 0xFF;
            sb.append(HEX[b >>> 4]);
            sb.append(HEX[b & 0x0F]);
            lc++;
        }

        return sb.toString();
    }

    public static String dumpValue(final HeaderValue<?> value) {
        final StringBuilder sb = new StringBuilder();
        dumpValue(sb, value.getValue());
        return sb.toString();
    }

    /**
     * Writes the contents of a {@link ByteBuffer} to an {@link OutputStream}. Note that this method will close the
     * output stream.
     *
     * @param stream the output stream
     * @param dataStore the data store
     * @throws IOException if some other I/O error occurs
     */
    static void writeByteBuffer(final OutputStream stream, final ByteBuffer dataStore) throws IOException {
        try (final WritableByteChannel c = Channels.newChannel(stream)) {
            while (dataStore.hasRemaining()) {
                c.write(dataStore);
            }
        }
    }

    public static <T> void dumpValue(final StringBuilder sb, final RpmTagValue<T> value) {
        sb.append(value);
    }

    public static int padding(final int offset) {
        final int rem = offset % 8;
        if (rem > 0) {
            return 8 - rem;
        } else {
            return 0;
        }
    }

}
