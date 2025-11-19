package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * JAXB DTO for <actor> elements; intentionally detached from the domain model.
 */
@XmlRootElement(name = "actor", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class ActorImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "createdBy")
    private String createdBy;

    @XmlElement(name = "name", namespace = "http://www.rreganjr.com/requel")
    private String name;

    @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
    private String text;

    @XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "annotationRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> annotationRefs = new ArrayList<>();

    @XmlElementWrapper(name = "goals", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "goalRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> goalRefs = new ArrayList<>();

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

    public List<String> getAnnotationRefs() {
        return annotationRefs;
    }

    public List<String> getGoalRefs() {
        return goalRefs;
    }

    public List<String> getGlossaryTermRefs() {
        return glossaryTermRefs;
    }
}
