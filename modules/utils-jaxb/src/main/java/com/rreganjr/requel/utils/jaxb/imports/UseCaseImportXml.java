package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "usecase", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class UseCaseImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "createdBy")
    private String createdBy;

    @XmlElement(name = "name", namespace = "http://www.rreganjr.com/requel")
    private String name;

    @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
    private String text;

    @XmlElement(name = "primaryActorRef", namespace = "http://www.rreganjr.com/requel")
    private String primaryActorRef;

    @XmlElement(name = "scenarioRef", namespace = "http://www.rreganjr.com/requel")
    private String scenarioRef;

    @XmlElementWrapper(name = "stories", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "storyRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> storyRefs = new ArrayList<>();

    @XmlElementWrapper(name = "goals", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "goalRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> goalRefs = new ArrayList<>();

    @XmlElementWrapper(name = "actors", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "actorRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> actorRefs = new ArrayList<>();

    @XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "annotationRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> annotationRefs = new ArrayList<>();

    public String getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public String getText() { return text; }
    public String getPrimaryActorRef() { return primaryActorRef; }
    public String getScenarioRef() { return scenarioRef; }
    public List<String> getStoryRefs() { return storyRefs; }
    public List<String> getGoalRefs() { return goalRefs; }
    public List<String> getActorRefs() { return actorRefs; }
    public List<String> getAnnotationRefs() { return annotationRefs; }
}
