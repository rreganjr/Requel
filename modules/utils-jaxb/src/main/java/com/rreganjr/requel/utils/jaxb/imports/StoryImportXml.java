package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "story", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class StoryImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "createdBy")
    private String createdBy;

    @XmlAttribute(name = "storyType")
    private String storyType;

    @XmlElement(name = "name", namespace = "http://www.rreganjr.com/requel")
    private String name;

    @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
    private String text;

    @XmlElementWrapper(name = "goals", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "goalRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> goalRefs = new ArrayList<>();

    @XmlElementWrapper(name = "actors", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "actorRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> actorRefs = new ArrayList<>();

    @XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "annotationRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> annotationRefs = new ArrayList<>();

    @XmlElementWrapper(name = "glossaryTerms", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "glossaryTermRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> glossaryTermRefs = new ArrayList<>();

    public String getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getStoryType() { return storyType; }
    public String getName() { return name; }
    public String getText() { return text; }
    public List<String> getGoalRefs() { return goalRefs; }
    public List<String> getActorRefs() { return actorRefs; }
    public List<String> getAnnotationRefs() { return annotationRefs; }
    public List<String> getGlossaryTermRefs() { return glossaryTermRefs; }
}
