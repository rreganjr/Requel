/*
 * $Id: VerbNetSelectionalRestrictionsParser.java,v 1.2 2009/02/09 10:12:28 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetRoleRef;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetSelectionRestrictionType;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditVerbNetSelectionRestrictionCommand;

/**
 * Parser for VerbNet thematic role selectional restrictions. Given a
 * VerbNetRoleRef, parse the selection restrictions xml and add a
 * VerbNetSelectionRestriction to the role reference.
 * 
 * @author ron
 */
@Component("verbNetSelectionalRestrictionsParser")
public class VerbNetSelectionalRestrictionsParser {
	private static final Logger log = Logger.getLogger(VerbNetFrameSyntaxParser.class);

	private final CommandHandler commandHandler;
	private final DictionaryCommandFactory dictionaryCommandFactory;
	private final DictionaryRepository dictionaryRepository;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public VerbNetSelectionalRestrictionsParser(CommandHandler commandHandler,
			DictionaryCommandFactory dictionaryCommandFactory,
			DictionaryRepository dictionaryRepository) {
		this.commandHandler = commandHandler;
		this.dictionaryCommandFactory = dictionaryCommandFactory;
		this.dictionaryRepository = dictionaryRepository;
	}

	/**
	 * @param roleRef -
	 *            the role reference the restriction applies to
	 * @param selrestrs -
	 *            a VerbNet selrestrs xml string
	 * @return the updated roleRef with the selectional restrictions added.
	 */
	public VerbNetRoleRef parseSelectionalRestrictions(VerbNetRoleRef roleRef) {
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Reader reader = new StringReader(roleRef.getRestrictionXML());
			Document selrestrsXML = db.parse(new InputSource(reader));
			NodeIterator nl = XPathAPI.selectNodeIterator(selrestrsXML, "/SELRESTRS/*");
			Node node;
			while ((node = nl.nextNode()) != null) {
				String tag = node.getNodeName();
				if ("SELRESTR".equals(tag)) {
					String value = "+";
					String typeName = null;
					if (node.getAttributes() != null) {
						if (node.getAttributes().getNamedItem("Value") != null) {
							value = node.getAttributes().getNamedItem("Value").getNodeValue();
						}
						if (node.getAttributes().getNamedItem("type") != null) {
							typeName = node.getAttributes().getNamedItem("type").getNodeValue();
						}
					}
					if ((typeName != null) && ("+".equals(value) || "-".equals(value))) {
						// TODO: this needs to be a command
						VerbNetSelectionRestrictionType type = getDictionaryRepository()
								.findVerbNetSelectionRestrictionType(typeName);
						EditVerbNetSelectionRestrictionCommand command = dictionaryCommandFactory
								.newEditVerbNetSelectionRestrictionCommand();
						command.setVerbNetRoleRef(roleRef);
						command.setVerbNetSelectionRestrictionType(type);
						command.setInclude(value);
						command = commandHandler.execute(command);
						roleRef = command.getVerbNetRoleRef();
					}
				} else {
					throw new Exception("Unexpected element type " + tag);
				}
			}
		} catch (Exception e) {
			log.error("could not parse thematic role selectional restriction for " + roleRef + ":"
					+ roleRef.getRestrictionXML(), e);
		}
		return roleRef;
	}

	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}
}
