/*
 * Copyright (c) 2015, 2022 Contributors to the Eclipse Foundation
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

import java.util.HashMap;
import java.util.Map;

public enum RpmTag implements RpmBaseTag {
    HEADER_SIGNATURES(62, byte[].class),
    HEADER_IMMUTABLE(63, byte[].class),
    HEADER_I18NTABLE(100, String.class),
    NAME(1000, String.class),
    VERSION(1001, String.class),
    RELEASE(1002, String.class),
    EPOCH(1003, Integer.class),
    SUMMARY(1004, String[].class),
    DESCRIPTION(1005, String[].class),
    BUILDTIME(1006, Integer.class),
    BUILDHOST(1007, String.class),
    SIZE(1009, Integer.class),
    DISTRIBUTION(1010, String.class),
    VENDOR(1011, String.class),
    LICENSE(1014, String.class),
    PACKAGER(1015, String.class),
    GROUP(1016, String[].class),
    URL(1020, String.class),
    OS(1021, String.class),
    ARCH(1022, String.class),
    PREINSTALL_SCRIPT(1023, String.class),
    POSTINSTALL_SCRIPT(1024, String.class),
    PREREMOVE_SCRIPT(1025, String.class),
    POSTREMOVE_SCRIPT(1026, String.class),
    FILE_SIZES(1028, Integer[].class),
    FILE_MODES(1030, Short[].class),
    FILE_RDEVS(1033, Short[].class),
    FILE_MTIMES(1034, Integer[].class),
    FILE_DIGESTS(1035, String[].class),
    FILE_LINKTO(1036, String[].class),
    FILE_FLAGS(1037, Integer[].class),
    FILE_USERNAME(1039, String[].class),
    FILE_GROUPNAME(1040, String[].class),
    SOURCE_PACKAGE(1044, String.class),
    FILE_VERIFYFLAGS(1045, Integer[].class),
    ARCHIVE_SIZE(1046, Integer.class),
    PROVIDE_NAME(1047, String[].class),
    REQUIRE_FLAGS(1048, Integer[].class),
    REQUIRE_NAME(1049, String[].class),
    REQUIRE_VERSION(1050, String[].class),
    CONFLICT_FLAGS(1053, Integer[].class),
    CONFLICT_NAME(1054, String[].class),
    CONFLICT_VERSION(1055, String[].class),
    RPMVERSION(1064, String.class),
    TRIGGER_SCRIPTS(1065, String[].class),
    TRIGGER_NAME(1066, String[].class),
    TRIGGER_VERSION(1067, String[].class),
    TRIGGER_FLAGS(1068, Integer[].class),
    TRIGGER_INDEX(1069, Integer[].class),
    VERIFY_SCRIPT(1079, String.class),
    CHANGELOG_TIMESTAMP(1080, Integer[].class),
    CHANGELOG_AUTHOR(1081, String[].class),
    CHANGELOG_TEXT(1082, String[].class),
    PREINSTALL_SCRIPT_PROG(1085, String[].class),
    POSTINSTALL_SCRIPT_PROG(1086, String[].class),
    PREREMOVE_SCRIPT_PROG(1087, String[].class),
    POSTREMOVE_SCRIPT_PROG(1088, String[].class),
    OBSOLETE_NAME(1090, String[].class),
    VERIFY_SCRIPT_PROG(1091, String[].class),
    TRIGGERSCRIPT_PROG(1092, String[].class),
    FILE_DEVICES(1095, Integer[].class),
    FILE_INODES(1096, Integer[].class),
    FILE_LANGS(1097, String[].class),
    PREFIXES(1098, String[].class),
    PROVIDE_FLAGS(1112, Integer[].class),
    PROVIDE_VERSION(1113, String[].class),
    OBSOLETE_FLAGS(1114, Integer[].class),
    OBSOLETE_VERSION(1115, String[].class),
    DIR_INDEXES(1116, Integer.class),
    BASENAMES(1117, String[].class),
    DIRNAMES(1118, String[].class),
    OPTFLAGS(1122, String.class),
    PAYLOAD_FORMAT(1124, String.class),
    PAYLOAD_CODING(1125, String.class),
    PAYLOAD_FLAGS(1126, String.class),
    PLATFORM(1132, String.class),
    PRETRANSACTION_SCRIPT(1151, String.class),
    POSTTRANSACTION_SCRIPT(1152, String.class),
    PRETRANSACTION_SCRIPT_PROG(1153, String[].class),
    POSTTRANSACTION_SCRIPT_PROG(1154, String[].class),
    /**
     * File size (when files &gt; 4GB are present). Always used in RPM 6.
     */
    LONG_FILE_SIZES(5008, Long[].class),
    LONGSIZE(5009, Long.class),
    FILE_DIGESTALGO(5011, Integer.class),
    RECOMMEND_NAME(5046, String[].class),
    RECOMMEND_VERSION(5047, String[].class),
    RECOMMEND_FLAGS(5048, Integer[].class),
    SUGGEST_NAME(5049, String[].class),
    SUGGEST_VERSION(5050, String[].class),
    SUGGEST_FLAGS(5051, Integer[].class),
    SUPPLEMENT_NAME(5052, String[].class),
    SUPPLEMENT_VERSION(5053, String[].class),
    SUPPLEMENT_FLAGS(5054, Integer[].class),
    ENHANCE_NAME(5055, String[].class),
    ENHANCE_VERSION(5056, String[].class),
    ENHANCE_FLAGS(5057, Integer[].class),
    /**
     * Encoding of the header string data. When present it is always "utf-8" and the data has actually been validated.
     * Always present in RPM 6.
     */
    ENCODING(5062, String.class),

    PAYLOAD_DIGEST(5092, String[].class),
    PAYLOAD_DIGEST_ALGO(5093, Integer.class),
    PAYLOAD_DIGEST_ALT(5097, String[].class),

    /**
     * The compressed payload size.
     */
    PAYLOAD_SIZE(5112, Long.class),
    /**
     * The uncompressed payload size.
     */
    PAYLOAD_SIZE_ALT(5113, Long.class),
    /**
     * The RPM version number (version 6 or later).
     */
    RPM_FORMAT(5114, Integer.class);

    private final Integer value;

    private final Class<?> dataType;

    <T> RpmTag(final Integer value, final Class<T> dataType) {
        this.value = value;
        this.dataType = dataType;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    public Class<?> getDataType() {
        return this.dataType;
    }

    private static final Map<Integer, RpmTag> all = new HashMap<>(RpmTag.values().length);

    static {
        for (final RpmTag tag : values()) {
            all.put(tag.getValue(), tag);
        }
    }

    public static RpmTag find(final Integer value) {
        return all.get(value);
    }

    @Override
    public String toString() {
        final RpmTag tag = find(this.value);
        return dataType.getSimpleName() + " " + (tag != null ? tag.name() + "(" + this.value + ")" : "UNKNOWN(" + this.value + ")");
    }
}
