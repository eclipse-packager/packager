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

public enum RpmFormat {
    RPM_3(3),
    RPM_4(4),
    RPM_6(6);

    public static final RpmFormat DEFAULT = RPM_4;

    private final int format;

    RpmFormat(final int format) {
        this.format = format;
    }

    public int getFormat() {
        return this.format;
    }

    public static RpmFormat fromFormat(final int format) {
        switch (format) {
            case 3:
                return RPM_3;
            case 4:
                return RPM_4;
            case 6:
                return RPM_6;
            default:
                throw new IllegalArgumentException("Unknown RPM format: " + format);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(this.format);
    }
}
