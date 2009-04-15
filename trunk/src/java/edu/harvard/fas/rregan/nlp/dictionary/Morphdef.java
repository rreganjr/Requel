/*
 * $Id: Morphdef.java,v 1.2 2009/01/03 10:24:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
