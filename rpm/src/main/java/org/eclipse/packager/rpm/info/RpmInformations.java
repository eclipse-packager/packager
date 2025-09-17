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

package org.eclipse.packager.rpm.info;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.eclipse.packager.rpm.RpmSignatureTag;
import org.eclipse.packager.rpm.RpmTag;
import org.eclipse.packager.rpm.RpmTagValue;
import org.eclipse.packager.rpm.info.RpmInformation.Dependency;
import org.eclipse.packager.rpm.parse.InputHeader;
import org.eclipse.packager.rpm.parse.RpmInputStream;

import static org.eclipse.packager.rpm.RpmSignatureTag.LONGARCHIVESIZE;
import static org.eclipse.packager.rpm.RpmSignatureTag.PAYLOAD_SIZE;
import static org.eclipse.packager.rpm.RpmTag.ARCH;
import static org.eclipse.packager.rpm.RpmTag.ARCHIVE_SIZE;
import static org.eclipse.packager.rpm.RpmTag.BUILDHOST;
import static org.eclipse.packager.rpm.RpmTag.BUILDTIME;
import static org.eclipse.packager.rpm.RpmTag.CHANGELOG_AUTHOR;
import static org.eclipse.packager.rpm.RpmTag.CHANGELOG_TEXT;
import static org.eclipse.packager.rpm.RpmTag.CHANGELOG_TIMESTAMP;
import static org.eclipse.packager.rpm.RpmTag.CONFLICT_FLAGS;
import static org.eclipse.packager.rpm.RpmTag.CONFLICT_NAME;
import static org.eclipse.packager.rpm.RpmTag.CONFLICT_VERSION;
import static org.eclipse.packager.rpm.RpmTag.DESCRIPTION;
import static org.eclipse.packager.rpm.RpmTag.EPOCH;
import static org.eclipse.packager.rpm.RpmTag.GROUP;
import static org.eclipse.packager.rpm.RpmTag.LICENSE;
import static org.eclipse.packager.rpm.RpmTag.LONGSIZE;
import static org.eclipse.packager.rpm.RpmTag.NAME;
import static org.eclipse.packager.rpm.RpmTag.OBSOLETE_FLAGS;
import static org.eclipse.packager.rpm.RpmTag.OBSOLETE_NAME;
import static org.eclipse.packager.rpm.RpmTag.OBSOLETE_VERSION;
import static org.eclipse.packager.rpm.RpmTag.PACKAGER;
import static org.eclipse.packager.rpm.RpmTag.PROVIDE_FLAGS;
import static org.eclipse.packager.rpm.RpmTag.PROVIDE_NAME;
import static org.eclipse.packager.rpm.RpmTag.PROVIDE_VERSION;
import static org.eclipse.packager.rpm.RpmTag.RELEASE;
import static org.eclipse.packager.rpm.RpmTag.REQUIRE_FLAGS;
import static org.eclipse.packager.rpm.RpmTag.REQUIRE_NAME;
import static org.eclipse.packager.rpm.RpmTag.REQUIRE_VERSION;
import static org.eclipse.packager.rpm.RpmTag.SIZE;
import static org.eclipse.packager.rpm.RpmTag.SOURCE_PACKAGE;
import static org.eclipse.packager.rpm.RpmTag.SUMMARY;
import static org.eclipse.packager.rpm.RpmTag.URL;
import static org.eclipse.packager.rpm.RpmTag.VENDOR;
import static org.eclipse.packager.rpm.RpmTag.VERSION;

public final class RpmInformations {
    private RpmInformations() {
    }

    /**
     * Returns the RPM information for the given RPM input stream.
     *
     * <p><em>Note that since version 0.21.0 <code>IllegalArgumentException</code> is thrown if an error occurs while reading the RPM header.</em></p>
     *
     * @param in the RPM input stream
     * @return the RPM information for the given RPM input stream
     * @throws IOException if an error occurs while reading from the given RPM input stream
     * @throws IllegalArgumentException if there are any problems reading the headers
     */
    public static RpmInformation makeInformation(final RpmInputStream in) throws IOException {
        final InputHeader<RpmTag> header = in.getPayloadHeader();
        final InputHeader<RpmSignatureTag> signature = in.getSignatureHeader();

        final RpmInformation result = new RpmInformation();

        result.setHeaderStart(header.getStart());
        result.setHeaderEnd(header.getStart() + header.getLength());

        result.setName(header.getString(NAME));
        result.setArchitecture(header.getString(ARCH));
        result.setSummary(header.getString(SUMMARY));
        result.setDescription(header.getString(DESCRIPTION));
        result.setPackager(header.getString(PACKAGER));
        result.setUrl(header.getString(URL));
        result.setLicense(header.getString(LICENSE));
        result.setVendor(header.getString(VENDOR));
        result.setGroup(header.getString(GROUP));

        result.setBuildHost(header.getString(BUILDHOST));
        result.setBuildTimestamp(RpmTagValue.toLong(header.getInteger(BUILDTIME)));
        result.setSourcePackage(header.getString(SOURCE_PACKAGE));

        result.setInstalledSize(RpmTagValue.toLong(header.getInteger(SIZE)));

        if (result.getInstalledSize() == null) {
            result.setInstalledSize(header.getLong(LONGSIZE));
        }

        result.setArchiveSize(RpmTagValue.toLong(header.getInteger(ARCHIVE_SIZE)));

        if (result.getArchiveSize() == null) {
            result.setArchiveSize(RpmTagValue.toLong(signature.getInteger(PAYLOAD_SIZE)));
        }

        if (result.getArchiveSize() == null) {
            result.setArchiveSize(signature.getLong(LONGARCHIVESIZE));
        }

        // version

        final RpmInformation.Version ver = new RpmInformation.Version(header.getString(VERSION), header.getString(RELEASE), header.getInteger(EPOCH));
        result.setVersion(ver);

        // changelog

        final List<Long> ts = RpmTagValue.toLong(header.getIntegerList(CHANGELOG_TIMESTAMP));

        if (ts != null) {
            final List<String> authors = header.getStringList(CHANGELOG_AUTHOR);
            final List<String> texts = header.getStringList(CHANGELOG_TEXT);
            final int size = ts.size();
            final List<RpmInformation.Changelog> changes = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                changes.add(new RpmInformation.Changelog(ts.get(i), authors.get(i), texts.get(i)));
            }

            changes.sort(Comparator.comparingLong(RpmInformation.Changelog::getTimestamp));

            result.setChangelog(changes);
        }

        // dependencies

        result.setProvides(makeDependencies(header, PROVIDE_NAME, PROVIDE_VERSION, PROVIDE_FLAGS));
        result.setRequires(makeDependencies(header, REQUIRE_NAME, REQUIRE_VERSION, REQUIRE_FLAGS));
        result.setConflicts(makeDependencies(header, CONFLICT_NAME, CONFLICT_VERSION, CONFLICT_FLAGS));
        result.setObsoletes(makeDependencies(header, OBSOLETE_NAME, OBSOLETE_VERSION, OBSOLETE_FLAGS));

        // files

        final CpioArchiveInputStream cpio = in.getCpioStream();
        CpioArchiveEntry cpioEntry;
        while ((cpioEntry = cpio.getNextEntry()) != null) {
            final String name = normalize(cpioEntry.getName());

            if (cpioEntry.isRegularFile()) {
                result.getFiles().add(name);
            } else if (cpioEntry.isDirectory()) {
                result.getDirectories().add(name);
            }
        }
        cpio.close();

        return result;
    }

    public static List<Dependency> makeDependencies(final InputHeader<RpmTag> header, final RpmTag namesTag, final RpmTag versionsTag, final RpmTag flagsTag) {
        final List<String> names = header.getStringList(namesTag);
        final List<String> versions = header.getStringList(versionsTag);
        final List<Integer> flags = header.getIntegerList(flagsTag);

        if (names == null) {
            return Collections.emptyList();
        }

        if (names.size() != versions.size()) {
            throw new IllegalStateException(String.format("Invalid size of dependency versions array [%s] - expected: %s, actual: %s", versionsTag, names.size(), versions.size()));
        }

        if (flags != null && names.size() != flags.size()) {
            throw new IllegalStateException(String.format("Invalid size of dependency flags array [%s] - expected: %s, actual: %s", flagsTag, names.size(), flags.size()));
        }

        final List<Dependency> result = new ArrayList<>(names.size());
        final Set<String> known = new HashSet<>();

        for (int i = 0; i < names.size(); i++) {
            final String name = names.get(i);
            String version = versions.get(i);

            if (version != null && version.isEmpty()) {
                version = null;
            }

            final Integer flag = flags != null ? flags.get(i) : null;

            if (known.add(name)) {
                result.add(new Dependency(name, version, flag != null ? flag : 0));
            }
        }

        return result;
    }

    public static String normalize(final String name) {
        if (name.startsWith("./")) {
            return name.substring(1);
        }

        return name;
    }
}
