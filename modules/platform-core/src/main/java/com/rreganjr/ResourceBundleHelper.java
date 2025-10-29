package com.rreganjr;
/*
 * $Id: $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
 * --- original notice ---
 *
 * This file is part of EchoPM - the Echo Panel Manager Framework
 *
 * EchoPM is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License version 1.1 (
 * "MPLv1.1") or later.
 *
 * Alternatively, EchoPM may be used under the terms of the GNU
 * General Public License version 3 ("GPLv3") or (at your option)
 * any later version, in which case the provisions of GPLv3 License
 * are applicable instead of the MPLv1.1 license.
 *
 * Alternatively, EchoPM may be used under the terms of the GNU
 * Lesser General Public License version 3 ("LGPLv3") or (at your
 * option) any later version, in which case the provisions of
 * LGPLv3 License are applicable instead of the MPLv1.1 license.
 *
 * If you wish to allow use of your version of EchoPM only under
 * the terms of the GPLv3 or LGPLv3 License and not to allow
 * others to use your version under the MPL, indicate your
 * decision by deleting the provisions above and replace them with
 * the notice and other provisions required by the GPLv3 License.
 * If you do not delete the provisions above, a recipient may use
 * your version of this file under either the MPLv1.1, GPLv3,
 * or LGPLv3 License or later versions of those licenses.
 *
 * EchoPM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * appropriate license for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * GNU Lesser General Public License, and Mozilla  along with EchoPM.
 * If not, see <http://www.gnu.org/licenses/> and
 * <http://www.mozilla.org/MPL/>.
 */

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * @author ron
 */
public class ResourceBundleHelper {
    private static final Logger log = Logger.getLogger(ResourceBundleHelper.class);

    private final String resourceBundleName;
    private ResourceBundle resourceBundle;

    /**
     * @param resourceBundleName
     */
    public ResourceBundleHelper(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
        try {
            setLocale(null);
        } catch (MissingResourceBundleException e) {
            log.warn("no resource file with bundle name " + resourceBundleName);
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     */
    public Integer getInteger(String key, Integer defaultValue) {
        try {
            return (Integer) getResourceBundle().getObject(key);
        } catch (ClassCastException e) {
            try {
                return Integer.parseInt(getResourceBundle().getString(key));
            } catch (Exception e2) {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     */
    public Double getDouble(String key, Double defaultValue) {
        try {
            return (Double) getResourceBundle().getObject(key);
        } catch (ClassCastException e) {
            try {
                return Double.parseDouble(getResourceBundle().getString(key));
            } catch (Exception e2) {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        try {
            return (Boolean) getResourceBundle().getObject(key);
        } catch (ClassCastException e) {
            try {
                return Boolean.parseBoolean(getResourceBundle().getString(key));
            } catch (Exception e2) {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @param key
     * @param defaultValue
     * @return
     */
    public String getString(String key, String defaultValue) {
        try {
            return getResourceBundle().getString(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @param key
     * @return
     */
    public String getString(String key) {
        return getResourceBundle().getString(key);
    }

    /**
     * Set the locale. This causes the resource bundle to be reloaded for the
     * new locale.
     *
     * @param locale
     */
    public void setLocale(Locale locale) {
        // TODO: don't reload the bundle if the same locale is set again
        try {
            if (locale != null) {
                log.debug("setting bundle " + getResourceBundleName() + " locale " + locale);
                setResourceBundle(ResourceBundle.getBundle(getResourceBundleName(), locale));
            } else {
                log.debug("setting bundle " + getResourceBundleName() + " using default locale");
                setResourceBundle(ResourceBundle.getBundle(getResourceBundleName()));
            }
        } catch (Exception e) {
            throw MissingResourceBundleException.missingResourceBundle(getResourceBundleName(), e);
        }
    }

    protected String getResourceBundleName() {
        return resourceBundleName;
    }

    private ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    private void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }
}
