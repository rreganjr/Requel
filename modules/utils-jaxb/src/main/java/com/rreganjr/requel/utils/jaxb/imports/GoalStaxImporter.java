package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.GoalImportDraft;
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
 * Streams <goal> elements from a project XML.
 */
@Component
public class GoalStaxImporter {

    private static final String NS = "http://www.rreganjr.com/requel";

    private final JAXBContext jaxbContext;
    private final GoalImportXmlMapper mapper;

    public GoalStaxImporter() {
        this(createJaxbContext(), new GoalImportXmlMapper());
    }

    public GoalStaxImporter(JAXBContext jaxbContext, GoalImportXmlMapper mapper) {
        this.jaxbContext = jaxbContext;
        this.mapper = mapper;
    }

    public List<GoalImportDraft> readGoals(InputStream inputStream) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<GoalImportDraft> drafts = new ArrayList<>();

            while (reader.hasNext()) {
                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT
                        && NS.equals(reader.getNamespaceURI())
                        && "goal".equals(reader.getLocalName())) {
                    GoalImportXml goalXml = unmarshaller.unmarshal(reader, GoalImportXml.class).getValue();
                    drafts.add(mapper.toDraft(goalXml));
                    continue;
                }
                reader.next();
            }
            reader.close();
            return drafts;
        } catch (XMLStreamException | JAXBException e) {
            throw new ImportException("Failed to stream goals from XML", e);
        }
    }

    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(GoalImportXml.class);
        } catch (JAXBException e) {
            throw new ImportException("Unable to initialize JAXB context for goals", e);
        }
    }
}
