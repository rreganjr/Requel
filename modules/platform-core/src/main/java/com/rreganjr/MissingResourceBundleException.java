/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr;

import lombok.extern.slf4j.Slf4j;
import java.util.Formatter;

@Slf4j
public class MissingResourceBundleException extends RuntimeException {
    protected static String MSG_MISSING_RESOURCE_BUNDLE = "The resource bundle '%s' could not be loaded: %s.";
    protected static String MSG_NO_RESOURCE_BUNDLE = "No resource bundle was supplied.";

    public static MissingResourceBundleException missingResourceBundle(String bundleName, Throwable cause) {
        return new MissingResourceBundleException(cause, MSG_MISSING_RESOURCE_BUNDLE, bundleName, cause);
    }

    public static MissingResourceBundleException missingResourceBundle() {
        return new MissingResourceBundleException(MSG_NO_RESOURCE_BUNDLE);
    }


    /**
     * @param format -
     *            a format string appropriate for java.util.Formatter
     * @param args -
     *            variable args list that map to the variables in the format
     *            string
     */
    protected MissingResourceBundleException(String format, Object... args) {
        super(new Formatter().format(format, pretty(args)).toString());
        if (log.isDebugEnabled()) {
            log.debug(getMessage());
        }
    }

    /**
     * @param cause -
     *            a caught exception that resulted in this exception
     * @param format -
     *            a format string appropriate for java.util.Formatter
     * @param args -
     *            variable args list that map to the variables in the format
     *            string
     */
    protected MissingResourceBundleException(Throwable cause, String format, Object... args) {
        super(new Formatter().format(format, pretty(args)).toString(), cause);
        if (log.isDebugEnabled()) {
            log.debug(getMessage(), cause);
        }
    }

    static private Object[] pretty(Object[] args) {
        Object[] pretty = new Object[args.length];
        int i = 0;
        for (Object o : args) {
            if (Object[].class.isAssignableFrom(o.getClass())) {
                StringBuilder b = new StringBuilder();
                Object[] inner = (Object[])args[i];
                for (int x = 0; x < inner.length - 1; x++) {
                    b.append(pretty(inner[x]));
                    b.append(", ");
                }
                b.append(pretty(inner[inner.length -1]));
                pretty[i] = b.toString();
            } else {
                pretty[i] = o.toString();
            }
            i++;
        }
        return pretty;
    }

    static private String pretty(Object o) {
        if (o == null) {
            return "<null>";
        } else if (o instanceof String) {
            return "\"" + o + "\"";
        } else {
            return o.toString();
        }
    }

}
