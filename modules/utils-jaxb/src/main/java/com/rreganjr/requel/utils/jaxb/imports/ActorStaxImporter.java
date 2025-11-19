package com.rreganjr.requel.utils.jaxb.imports;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.ActorImportDraft;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;

/**
 * Streams <actor> elements from a project XML without touching domain entities.
 */
@Component
public class ActorStaxImporter {

    private static final String NS = "http://www.rreganjr.com/requel";

    private final JAXBContext jaxbContext;
    private final ActorImportXmlMapper mapper;

    public ActorStaxImporter() {
        this(createJaxbContext(), new ActorImportXmlMapper());
    }

    public ActorStaxImporter(JAXBContext jaxbContext, ActorImportXmlMapper mapper) {
        this.jaxbContext = jaxbContext;
        this.mapper = mapper;
    }

    public List<ActorImportDraft> readActors(InputStream inputStream) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<ActorImportDraft> drafts = new ArrayList<>();

            while (reader.hasNext()) {
                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT
                        && NS.equals(reader.getNamespaceURI())
                        && "actor".equals(reader.getLocalName())) {
                    ActorImportXml actorXml = unmarshaller.unmarshal(reader, ActorImportXml.class).getValue();
                    drafts.add(mapper.toDraft(actorXml));
                    continue;
                }
                reader.next();
            }
            reader.close();
            return drafts;
        } catch (XMLStreamException | JAXBException e) {
            throw new ImportException("Failed to stream actors from XML", e);
        }
    }

    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(ActorImportXml.class);
        } catch (JAXBException e) {
            throw new ImportException("Unable to initialize JAXB context for actors", e);
        }
    }
}
