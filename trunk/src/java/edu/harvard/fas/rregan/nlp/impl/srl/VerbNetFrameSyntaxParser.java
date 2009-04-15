/*
 * $Id: VerbNetFrameSyntaxParser.java,v 1.7 2009/02/11 09:02:55 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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

import edu.harvard.fas.rregan.nlp.SemanticRole;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetFrame;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetFrameRef;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetRoleRef;

/**
 * Parser for VerbNet frame syntax xml strings.<br>
 * NOTE: selectional restriction elements are ignored.
 * 
 * @author ron
 */
@Component("verbNetFrameSyntaxParser")
public class VerbNetFrameSyntaxParser {
	private static final Logger log = Logger.getLogger(VerbNetFrameSyntaxParser.class);

	private final DictionaryRepository dictionaryRepository;
	private final VerbNetSelectionalRestrictionsParser vnSelResParser;

	/**
	 * @param dictionaryRepository
	 * @param vnSelResParser
	 */
	@Autowired
	public VerbNetFrameSyntaxParser(DictionaryRepository dictionaryRepository,
			VerbNetSelectionalRestrictionsParser vnSelResParser) {
		this.dictionaryRepository = dictionaryRepository;
		this.vnSelResParser = vnSelResParser;
	}

	/**
	 * @param frameRef -
	 *            a VerbNet Frame reference
	 * @return
	 */
	public List<SyntaxMatchingRule> parseVerbNetFrameSyntax(VerbNetFrameRef frameRef) {
		VerbNetFrame frame = frameRef.getFrame();
		List<SyntaxMatchingRule> rules = new ArrayList<SyntaxMatchingRule>();
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Reader reader = new StringReader("<syntax>" + frame.getSyntax() + "</syntax>");
			Document verbNetSyntaxXML = db.parse(new InputSource(reader));
			NodeIterator nl = XPathAPI.selectNodeIterator(verbNetSyntaxXML, "/syntax/*");
			Node node;
			while ((node = nl.nextNode()) != null) {
				String tag = node.getNodeName();
				if ("NP".equals(tag)) {
					String roleName = null;
					if ((node.getAttributes() != null)
							&& (node.getAttributes().getNamedItem("value") != null)) {
						roleName = node.getAttributes().getNamedItem("value").getNodeValue();
						SemanticRole role = SemanticRole.valueOf(roleName.toUpperCase());
						if (role == null) {
							throw SemanticRoleLabelerException.unknownSemanticRole(roleName,
									frameRef);
						}
						VerbNetRoleRef roleRef = frameRef.getVnClass().getRoleRef(roleName);
						if (roleRef == null) {
							throw SemanticRoleLabelerException.roleNotInClass(roleName, frameRef);
						}
						if ((roleRef.getRestrictionXML() != null)
								&& (roleRef.getRestrictionXML().length() > 0)
								&& (roleRef.getSelectionalRestrictions().size() == 0)) {
							roleRef = vnSelResParser.parseSelectionalRestrictions(roleRef);
						}
						rules.add(new NounPhraseMatchingRule(roleRef));
					}
				} else if ("VERB".equals(tag)) {
					rules.add(new VerbMatchingRule());
				} else if ("ADJ".equals(tag)) {
					rules.add(new AdjectiveMatchingRule());
				} else if ("ADV".equals(tag)) {
					rules.add(new AdverbMatchingRule());
				} else if ("PREP".equals(tag)) {
					if ((node.getAttributes() != null)
							&& (node.getAttributes().getNamedItem("value") != null)) {
						String filter = node.getAttributes().getNamedItem("value").getNodeValue();
						rules.add(new PrepositionMatchingRule(filter));
					} else {
						rules.add(new PrepositionMatchingRule());
					}
				} else if ("LEX".equals(tag)) {
					if ((node.getAttributes() != null)
							&& (node.getAttributes().getNamedItem("value") != null)) {
						String filter = node.getAttributes().getNamedItem("value").getNodeValue();
						rules.add(new LexicalMatchingRule(filter));
					}
				} else {
					throw new Exception("Unexpected element type " + tag);
				}
			}
		} catch (Exception e) {
			log.error("could not parse frame " + frame + " syntax: " + frame.getSyntax(), e);
			rules.clear();
		}
		return rules;
	}

	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}
}
