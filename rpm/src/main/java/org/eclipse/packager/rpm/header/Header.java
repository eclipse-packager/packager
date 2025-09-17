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

package org.eclipse.packager.rpm.header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.eclipse.packager.rpm.ReadableHeader;
import org.eclipse.packager.rpm.RpmBaseTag;
import org.eclipse.packager.rpm.RpmTag;

public class Header<T extends RpmBaseTag> implements ReadableHeader<T> {
    @FunctionalInterface
    public interface ArrayAllocator<T> {
        T[] allocate(int length);
    }

    @FunctionalInterface
    public interface Putter<T extends RpmBaseTag, V> {
        void put(Header<T> header, T tag, V[] values);
    }

    @FunctionalInterface
    public interface ToShortFunction<T> {
        short applyAsShort(T value);
    }

    private static final class I18nString {
        private final String value;

        public I18nString(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Objects.toString(this.value);
        }
    }

    private final Map<Integer, HeaderEntry<?>> entries = new LinkedHashMap<>();

    private final Charset charset;

    public Header(final HeaderEntry<?>[] entries) {
        this(entries, StandardCharsets.UTF_8);
    }

    public Header(final HeaderEntry<?>[] entries, final Charset charset) {
        Objects.requireNonNull(charset);

        this.charset = charset;

        if (entries != null) {
            for (final HeaderEntry<?> entry : entries) {
                this.entries.put(entry.getTag(), entry);
            }
        }
    }

    public Header(final Header<T> other) {
        Objects.requireNonNull(other);

        this.entries.putAll(other.entries);
        this.charset = other.charset;
    }

    public Header() {
        this.charset = StandardCharsets.UTF_8;
    }

    public int size() {
        return this.entries.size();
    }

    public void putNull(final int tag) {
        this.entries.put(tag, null);
    }

    public void putNull(final T tag) {
        this.entries.put(tag.getValue(), null);
    }

    public void putByte(final int tag, final byte... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putByte(final T tag, final byte... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putShort(final int tag, final short... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putShort(final T tag, final short... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putInt(final int tag, final int... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putInt(final T tag, final int... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putLong(final int tag, final long... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putLong(final T tag, final long... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putString(final int tag, final String value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putString(final T tag, final String value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putStringOptional(final int tag, final String value) {
        if (value == null) {
            return;
        }

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putStringOptional(final T tag, final String value) {
        if (value == null) {
            return;
        }

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putStringArray(final int tag, final String... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putStringArray(final T tag, final String... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putI18nString(final int tag, final String... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, Arrays.stream(value).map(I18nString::new).toArray(I18nString[]::new)));
    }

    public void putI18nString(final T tag, final String... value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), Arrays.stream(value).map(I18nString::new).toArray(I18nString[]::new)));
    }

    public void putBlob(final int tag, final byte[] value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, ByteBuffer.wrap(value)));
    }

    public void putBlob(final int tag, final ByteBuffer value) {
        Objects.requireNonNull(value);

        this.entries.put(tag, makeEntry(tag, value));
    }

    public void putBlob(final T tag, final byte[] value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), ByteBuffer.wrap(value)));
    }

    public void putBlob(final T tag, final ByteBuffer value) {
        Objects.requireNonNull(value);

        this.entries.put(tag.getValue(), makeEntry(tag.getValue(), value));
    }

    public void putSize(long value, final T intTag, final T longTag, final int rpmFormat) {
        Objects.requireNonNull(intTag);
        Objects.requireNonNull(longTag);

        if (value <= 0) {
            value = 0;
        }

        if (rpmFormat < 6 && value < Integer.MAX_VALUE) {
            putInt(intTag, (int) value);
        } else {
            putLong(longTag, value);
        }
    }

    public long getSize(final T intTag, final T longTag) {
        if (hasTag(longTag)) {
            return getLong(longTag);
        } else if (hasTag(intTag)) {
            return getInteger(intTag);
        } else {
            throw new IllegalArgumentException("Size not found!");
        }
    }

    public void putAll(final Header<T> headers) {
        this.entries.putAll(headers.entries);
    }

    public void remove(final int tag) {
        this.entries.remove(tag);
    }

    public void remove(final RpmTag tag) {
        this.entries.remove(tag.getValue());
    }

    public HeaderEntry<?> get(final int tag) {
        return this.entries.get(tag);
    }

    public HeaderEntry<?> get(final T tag) {
        return this.entries.get(tag.getValue());
    }

    @Override
    public boolean hasTag(final T tag) {
        return this.entries.containsKey(tag.getValue());
    }

    @Override
    public String getString(T tag) {
        if (!String.class.isAssignableFrom(tag.getDataType()) && !String[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not a string or array of strings");
        }

        return get(tag).getValue().asString().orElse(null);
    }

    @Override
    public Integer getInteger(T tag) {
        if (!Integer.class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an integer");
        }

        return get(tag).getValue().asInteger().orElse(null);
    }

    public Long getLong(T tag) {
        if (!Long.class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not a long");
        }

        return get(tag).getValue().asLong().orElse(null);
    }

    @Override
    public List<String> getStringList(T tag) {
        if (!String[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of strings");
        }

        return get(tag).getValue().asStringArray().map(Arrays::asList).orElse(null);
    }

    @Override
    public List<Integer> getIntegerList(T tag) {
        if (!Integer[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of integers");
        }

        return get(tag).getValue().asIntegerArray().map(Arrays::asList).orElse(null);
    }

    @Override
    public List<Long> getLongList(T tag) {
        if (!Long[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of longs");
        }

        return get(tag).getValue().asLongArray().map(Arrays::asList).orElse(null);
    }

    @Override
    public byte[] getByteArray(T tag) {
        if (!byte[].class.isAssignableFrom(tag.getDataType())) {
            throw new IllegalArgumentException("Tag " + tag  + " is not an array of bytes");
        }

        return get(tag).getValue().asByteArray().orElse(null);
    }

    /**
     * Make an array of header entries with given charset
     * <p>
     * <strong>Note:</strong> Further updates on this instance will not update
     * the returned array. This is actually a copy of the current state.
     * </p>
     *
     * @param charset the charset of choice
     * @return a new array of all header entries, unsorted
     */
    public HeaderEntry<?>[] makeEntries(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("'charset' cannot be null");
        }
        return this.entries.entrySet().stream().map(entry -> makeEntry(entry, charset)).toArray(HeaderEntry<?>[]::new);
    }

    private HeaderEntry<?> makeEntry(final Map.Entry<Integer, HeaderEntry<?>> entry) {
        return makeEntry(entry, this.charset);
    }

    /**
     * Make an array of header entries
     * <p>
     * <strong>Note:</strong> Further updates on this instance will not update
     * the returned array. This is actually a copy of the current state.
     * </p>
     *
     * @return a new array of all header entries, unsorted
     */
    public HeaderEntry<?>[] makeEntries() {
        return makeEntries(StandardCharsets.UTF_8);
    }

    private static HeaderEntry<?> makeEntry(final Map.Entry<Integer, HeaderEntry<?>> entry, final Charset charset) {
        return entry.getValue();
    }

    private static <E> HeaderEntry<E> makeEntry(int tag, E val) {
        return makeEntry(tag, val, StandardCharsets.UTF_8);
    }

    private static <E> HeaderEntry<E> makeEntry(int tag, E val, Charset charset) {
        // NULL
        if (val == null) {
            return new HeaderEntry<>(Type.NULL, tag, 0, null, val);
        }

        // FIXME: CHAR

        // BYTE

        if (val instanceof byte[]) {
            final byte[] value = (byte[]) val;
            return new HeaderEntry<>(Type.BYTE, tag, value.length, value, val);
        }

        // SHORT

        if (val instanceof short[]) {
            final short[] value = (short[]) val;

            final byte[] data = new byte[value.length * Short.BYTES];
            final ByteBuffer buffer = ByteBuffer.wrap(data);
            for (final short v : value) {
                buffer.putShort(v);
            }

            return new HeaderEntry<>(Type.SHORT, tag, value.length, data, val);
        }

        // INT

        if (val instanceof int[]) {
            final int[] value = (int[]) val;

            final byte[] data = new byte[value.length * Integer.BYTES];
            final ByteBuffer buffer = ByteBuffer.wrap(data);
            for (final int v : value) {
                buffer.putInt(v);
            }

            return new HeaderEntry<>(Type.INT, tag, value.length, data, val);
        }

        // LONG

        if (val instanceof long[]) {
            final long[] value = (long[]) val;

            final byte[] data = new byte[value.length * Long.BYTES];
            final ByteBuffer buffer = ByteBuffer.wrap(data);
            for (final long v : value) {
                buffer.putLong(v);
            }

            return new HeaderEntry<>(Type.LONG, tag, value.length, data, val);
        }

        // STRING

        if (val instanceof String) {
            final String value = (String) val;

            return new HeaderEntry<>(Type.STRING, tag, 1, makeStringData(new ByteArrayOutputStream(), value, charset).toByteArray(), val);
        }

        // BLOB

        if (val instanceof ByteBuffer) {
            final ByteBuffer value = (ByteBuffer) val;
            byte[] data;
            if (value.hasArray()) {
                data = value.array();
            } else {
                data = new byte[value.remaining()];
                value.get(data);
            }
            return new HeaderEntry<>(Type.BLOB, tag, data.length, data, val);
        }

        // STRING_ARRAY

        if (val instanceof String[]) {
            final String[] value = (String[]) val;

            return new HeaderEntry<>(Type.STRING_ARRAY, tag, value.length, makeStringsData(new ByteArrayOutputStream(), value, charset).toByteArray(), val);
        }

        // I18N_STRING

        if (val instanceof I18nString[]) {
            final I18nString[] value = (I18nString[]) val;

            return new HeaderEntry<>(Type.I18N_STRING, tag, value.length, makeStringsData(new ByteArrayOutputStream(), value, charset).toByteArray(), val);
        }

        throw new IllegalArgumentException(String.format("Unable to process value type: %s", val.getClass()));
    }

    private static <T extends OutputStream> T makeStringData(final T out, final String string, final Charset charset) {
        try {
            if (string != null) {
                out.write(string.getBytes(charset));
            }
            out.write(0);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    private static <T extends OutputStream> T makeStringsData(final T out, final String[] strings, final Charset charset) {
        for (final String s : strings) {
            makeStringData(out, s, charset);
        }
        return out;
    }

    private static <T extends OutputStream> T makeStringsData(final T out, final I18nString[] strings, final Charset charset) {
        for (final I18nString s : strings) {
            if (s != null) {
                makeStringData(out, s.value, charset);
            } else {
                makeStringData(out, null, charset);
            }
        }
        return out;
    }

    public static <E, V, T extends RpmBaseTag> void putFields(final Header<T> header, final Collection<E> entries, final T tag, final ArrayAllocator<V> arrayAllocator, final Function<E, V> func, final Putter<T, V> putter) {
        if (entries.isEmpty()) {
            return;
        }

        final V[] values = arrayAllocator.allocate(entries.size());
        int i = 0;
        for (final E entry : entries) {
            values[i] = func.apply(entry);
            i++;
        }

        putter.put(header, tag, values);
    }

    public static <E, T extends RpmBaseTag> void putShortFields(final Header<T> header, final Collection<E> entries, final T tag, final ToShortFunction<E> func) {
        if (entries.isEmpty()) {
            return;
        }

        final short[] values = new short[entries.size()];
        int i = 0;
        for (final E entry : entries) {
            values[i] = func.applyAsShort(entry);
            i++;
        }

        header.putShort(tag, values);
    }

    public static <E, T extends RpmBaseTag> void putIntFields(final Header<T> header, final Collection<E> entries, final T tag, final ToIntFunction<E> func) {
        if (entries.isEmpty()) {
            return;
        }

        final int[] values = new int[entries.size()];
        int i = 0;
        for (final E entry : entries) {
            values[i] = func.applyAsInt(entry);
            i++;
        }

        header.putInt(tag, values);
    }

    public static <E, T extends RpmBaseTag> void putLongFields(final Header<T> header, final Collection<E> entries, final T tag, final ToLongFunction<E> func) {
        if (entries.isEmpty()) {
            return;
        }

        final long[] values = new long[entries.size()];
        int i = 0;
        for (final E entry : entries) {
            values[i] = func.applyAsLong(entry);
            i++;
        }

        header.putLong(tag, values);
    }
}
