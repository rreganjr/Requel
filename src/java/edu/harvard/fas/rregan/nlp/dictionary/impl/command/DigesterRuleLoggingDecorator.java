/*
 * $Id: DigesterRuleLoggingDecorator.java,v 1.1 2008/12/13 00:39:57 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * Decorates a WordNetTaggedGlossaryDigesterRule to log begin(), body() and
 * end() method calls before (after for end) calling the same method on the
 * decorated rule
 * 
 * @author ron
 */
public class DigesterRuleLoggingDecorator extends Rule {
	private static final Logger log = Logger.getLogger(DigesterRuleLoggingDecorator.class);

	private Rule rule = null;

	/**
	 * Create an instance that doesn't decorate another digester rule and justs
	 * logs begin(), body(), and end() events.
	 */
	public DigesterRuleLoggingDecorator() {
		super();
	}

	/**
	 * Create an instance that decorates the supplied digester rule and logs
	 * begin(), body(), and end() events and calls the same method on the
	 * decorated rule.
	 * 
	 * @param rule -
	 *            the rule being decorated.
	 */
	public DigesterRuleLoggingDecorator(Rule rule) {
		super();
		setRule(rule);
	}

	@Override
	public void begin(String namespace, String name, Attributes attributes) throws Exception {

		if (log.isDebugEnabled()) {
			log.debug("starting element: name = \"" + name + "\" namespace = \"" + namespace
					+ "\" attributes = " + attributesToString(attributes));
		}
		if (getRule() != null) {
			getRule().begin(namespace, name, attributes);
		}
	}

	@Override
	public void body(String namespace, String name, String body) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("body of element: name = \"" + name + "\" namespace = \"" + namespace
					+ "\" body = " + body);
		}
		if (getRule() != null) {
			getRule().body(namespace, name, body);
		}
	}

	@Override
	public void end(String namespace, String name) throws Exception {
		if (getRule() != null) {
			getRule().end(namespace, name);
		}
		if (log.isDebugEnabled()) {
			log.debug("ending element: name = \"" + name + "\" namespace = \"" + namespace + "\"");
		}
	}

	@Override
	public Digester getDigester() {
		return getRule().getDigester();
	}

	@Override
	public void setDigester(Digester digester) {
		getRule().setDigester(digester);
	}

	private Rule getRule() {
		return rule;
	}

	private void setRule(Rule rule) {
		this.rule = rule;
	}

	private String attributesToString(Attributes attributes) {
		StringBuffer strbuf = new StringBuffer(255);
		strbuf.append("[");
		if ((attributes != null) && (attributes.getLength() > 0)) {
			strbuf.append(attributes.getQName(0));
			strbuf.append(" = \"");
			strbuf.append(attributes.getValue(0));
			strbuf.append("\"");
			for (int i = 1; i < attributes.getLength(); i++) {
				strbuf.append(", ");
				strbuf.append(attributes.getQName(i));
				strbuf.append(" = \"");
				strbuf.append(attributes.getValue(i));
				strbuf.append("\"");
			}
			strbuf.append("]");
		}
		return strbuf.toString();
	}
}
