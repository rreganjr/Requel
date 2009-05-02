/*
 * $Id: Morphdef.java,v 1.2 2009/01/03 10:24:32 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * '
 * 
 * @author ron
 */
@Entity
@Table(name = "morphdef", uniqueConstraints = @UniqueConstraint(columnNames = "lemma"))
public class Morphdef implements Serializable {
	static final long serialVersionUID = 0;

	private int morphid;
	private String lemma;

	public Morphdef() {
	}

	public Morphdef(int morphid, String lemma) {
		this.morphid = morphid;
		this.lemma = lemma;
	}

	@Id
	@Column(name = "morphid", unique = true, nullable = false)
	public int getMorphid() {
		return this.morphid;
	}

	public void setMorphid(int morphid) {
		this.morphid = morphid;
	}

	@Column(name = "lemma", unique = true)
	public String getLemma() {
		return this.lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

}
