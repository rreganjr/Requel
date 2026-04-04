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

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.annotation.AnnotationImportDraft;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

@Component
public class AnnotationStaxImporter {

    private static final String NS = "http://www.rreganjr.com/requel";
    private final JAXBContext jaxbContext;
    private final AnnotationImportXmlMapper mapper;

    public AnnotationStaxImporter() {
        this(createJaxbContext(), new AnnotationImportXmlMapper());
    }

    public AnnotationStaxImporter(JAXBContext jaxbContext, AnnotationImportXmlMapper mapper) {
        this.jaxbContext = jaxbContext;
        this.mapper = mapper;
    }

    public List<AnnotationImportDraft> readAnnotations(InputStream inputStream) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<AnnotationImportDraft> drafts = new ArrayList<>();

            while (reader.hasNext()) {
                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT
                        && NS.equals(reader.getNamespaceURI())) {
                    if ("note".equals(reader.getLocalName())) {
                        AnnotationImportXml xml = unmarshaller.unmarshal(reader, AnnotationImportXml.class).getValue();
                        drafts.add(mapper.toDraft(xml, AnnotationImportDraft.Type.NOTE));
                        continue;
                    } else if ("lexicalIssue".equals(reader.getLocalName())) {
                        AnnotationImportXml xml = unmarshaller.unmarshal(reader, AnnotationImportXml.class).getValue();
                        drafts.add(mapper.toDraft(xml, AnnotationImportDraft.Type.LEXICAL_ISSUE));
                        continue;
                    }
                }
                reader.next();
            }
            reader.close();
            return drafts;
        } catch (XMLStreamException | JAXBException e) {
            throw new ImportException("Failed to stream annotations from XML", e);
        }
    }

    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(AnnotationImportXml.class);
        } catch (JAXBException e) {
            throw new ImportException("Unable to initialize JAXB context for annotations", e);
        }
    }
}
