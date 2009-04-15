/*
 * $Id: PropertyContainerSupport.java,v 1.2 2008/02/15 21:59:08 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils;

import java.util.HashMap;
import java.util.Map;

public class PropertyContainerSupport implements PropertyContainer {

    private Map<String, Object> properties = new HashMap<String,Object>();

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Object getProperty(String key, Object defaultValue) {
    	Object value = properties.get(key);
    	if (value != null) {
    		return value;
    	}
    	return defaultValue;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> copy = new HashMap<String,Object>(properties.size());
        copy.putAll(properties);
        return copy;
    }

    public void setProperties(Map<String, Object> properties) {
        if (properties == null) {
            this.properties.clear();
        } else {
            this.properties = new HashMap<String,Object>(properties.size());
            addProperties(properties);
        }
    }

    public void addProperties(Map<String, Object> properties) {
        this.properties.putAll(properties);
    }
}
