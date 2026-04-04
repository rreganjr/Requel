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
package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "annotation", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class AnnotationImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "createdBy")
    private String createdBy;

    @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
    private String text;

    @XmlAttribute(name = "mustBeResolved")
    private Boolean mustBeResolved;

    @XmlAttribute(name = "word")
    private String word;

    @XmlAttribute(name = "propertyName")
    private String annotatablePropertyName;

    @XmlElementWrapper(name = "positions", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "positionRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> positionRefs = new ArrayList<>();

    @XmlElementWrapper(name = "annotatables", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "annotatableRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> annotatableRefs = new ArrayList<>();

    // differentiate by element name externally (lexicalIssue vs note)
    public String getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getText() { return text; }
    public Boolean getMustBeResolved() { return mustBeResolved; }
    public String getWord() { return word; }
    public String getAnnotatablePropertyName() { return annotatablePropertyName; }
    public List<String> getPositionRefs() { return positionRefs; }
    public List<String> getAnnotatableRefs() { return annotatableRefs; }
}
