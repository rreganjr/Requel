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
    public List<String> getPositionRefs() { return positionRefs; }
    public List<String> getAnnotatableRefs() { return annotatableRefs; }
}
