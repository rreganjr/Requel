package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "position", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class PositionImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "createdBy")
    private String createdBy;

    @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
    private String text;

    @XmlElementWrapper(name = "arguments", namespace = "http://www.rreganjr.com/requel")
    @XmlElement(name = "argument", namespace = "http://www.rreganjr.com/requel")
    private List<ArgumentImportXml> arguments = new ArrayList<>();

    public String getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public String getText() { return text; }
    public List<ArgumentImportXml> getArguments() { return arguments; }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ArgumentImportXml {
        @XmlAttribute(name = "id")
        private String id;
        @XmlElement(name = "text", namespace = "http://www.rreganjr.com/requel")
        private String text;
        public String getId() { return id; }
        public String getText() { return text; }
    }
}
