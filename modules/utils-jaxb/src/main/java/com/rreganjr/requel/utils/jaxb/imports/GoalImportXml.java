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

@XmlRootElement(name = "goal", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class GoalImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "createdBy")
    private String createdBy;

    @XmlElement(name = "name", namespace = "http://www.rreganjr.com/requel")
    private String name;

    @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
    private String text;

    @XmlElementWrapper(name = "goalRelations", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "goalRelation", namespace = "http://www.rreganjr.com/requel")
    private List<GoalRelationXml> goalRelations = new ArrayList<>();

    @XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "annotationRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> annotationRefs = new ArrayList<>();

    @XmlElementWrapper(name = "glossaryTerms", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "glossaryTermRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> glossaryTermRefs = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public List<GoalRelationXml> getGoalRelations() {
        return goalRelations;
    }

    public List<String> getAnnotationRefs() {
        return annotationRefs;
    }

    public List<String> getGlossaryTermRefs() {
        return glossaryTermRefs;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GoalRelationXml {
        @XmlAttribute(name = "relationType")
        private String relationType;

        @XmlAttribute(name = "toGoal")
        private String toGoal;

        public String getRelationType() {
            return relationType;
        }

        public String getToGoal() {
            return toGoal;
        }
    }
}
