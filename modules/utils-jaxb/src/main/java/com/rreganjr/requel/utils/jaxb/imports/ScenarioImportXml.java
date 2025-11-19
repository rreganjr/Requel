package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "scenario", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class ScenarioImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "createdBy")
    private String createdBy;

    @XmlAttribute(name = "scenarioType")
    private String scenarioType;

    @XmlElement(name = "name", namespace = "http://www.rreganjr.com/requel")
    private String name;

    @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
    private String text;

    @XmlElementWrapper(name = "steps", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "stepRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> stepRefs = new ArrayList<>();

    @XmlElementWrapper(name = "annotations", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "annotationRef", namespace = "http://www.rreganjr.com/requel")
    private List<String> annotationRefs = new ArrayList<>();

    public String getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getScenarioType() { return scenarioType; }
    public String getName() { return name; }
    public String getText() { return text; }
    public List<String> getStepRefs() { return stepRefs; }
    public List<String> getAnnotationRefs() { return annotationRefs; }
}
