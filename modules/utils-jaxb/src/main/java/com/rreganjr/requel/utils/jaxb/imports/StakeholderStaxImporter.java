/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
import com.rreganjr.requel.imports.identity.UserImportDraft;
import com.rreganjr.requel.imports.project.StakeholderImportDraft;
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

/**
 * Streams user-stakeholder elements and exposes both stakeholder and user drafts.
 */
@Component
public class StakeholderStaxImporter {

    private static final String NS = "http://www.rreganjr.com/requel";
    private final JAXBContext jaxbContext;
    private final StakeholderImportXmlMapper mapper;

    public StakeholderStaxImporter() {
        this(createJaxbContext(), new StakeholderImportXmlMapper());
    }

    public StakeholderStaxImporter(JAXBContext jaxbContext, StakeholderImportXmlMapper mapper) {
        this.jaxbContext = jaxbContext;
        this.mapper = mapper;
    }

    public StakeholderReadResult readStakeholders(InputStream inputStream) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<StakeholderImportDraft> stakeholderDrafts = new ArrayList<>();
            List<UserImportDraft> userDrafts = new ArrayList<>();

            while (reader.hasNext()) {
                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && NS.equals(reader.getNamespaceURI())) {
                    if ("user-stakeholder".equals(reader.getLocalName())) {
                        StakeholderImportXml stakeholderXml = unmarshaller.unmarshal(reader, StakeholderImportXml.class).getValue();
                        stakeholderDrafts.add(mapper.toDraft(stakeholderXml));
                        userDrafts.add(mapper.toUserDraft(stakeholderXml));
                        continue;
                    } else if ("nonuser-stakeholder".equals(reader.getLocalName())) {
                        NonUserStakeholderImportXml xml = unmarshaller.unmarshal(reader, NonUserStakeholderImportXml.class).getValue();
                        stakeholderDrafts.add(mapper.toDraft(xml));
                        continue;
                    }
                }
                reader.next();
            }
            reader.close();
            return new StakeholderReadResult(stakeholderDrafts, userDrafts);
        } catch (XMLStreamException | JAXBException e) {
            throw new ImportException("Failed to stream stakeholders from XML", e);
        }
    }

    public record StakeholderReadResult(List<StakeholderImportDraft> stakeholders, List<UserImportDraft> users) { }

    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(StakeholderImportXml.class, NonUserStakeholderImportXml.class);
        } catch (JAXBException e) {
            throw new ImportException("Unable to initialize JAXB context for stakeholders", e);
        }
    }
}
