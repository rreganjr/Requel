/*
 * $Id: SenseId.java,v 1.1 2008/12/13 00:40:36 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Wordnet Sense composite id based on a Word and a Synset.
 */
@Embeddable
public class SenseId implements Serializable {
	static final long serialVersionUID = 0;

	private Long synsetid;
	private Long wordid;

	public SenseId() {
	}

	public SenseId(Long synsetid, Long wordid) {
		this.synsetid = synsetid;
		this.wordid = wordid;
	}

	@Column(name = "synsetid", nullable = false)
	public Long getSynsetid() {
		return this.synsetid;
	}

	public void setSynsetid(Long synsetid) {
		this.synsetid = synsetid;
	}

	@Column(name = "wordid", nullable = false)
	public Long getWordid() {
		return this.wordid;
	}

	public void setWordid(Long wordid) {
		this.wordid = wordid;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other)) {
			return true;
		}
		if ((other == null)) {
			return false;
		}
		if (!(other instanceof SenseId)) {
			return false;
		}
		SenseId castOther = (SenseId) other;

		return (this.getSynsetid() == castOther.getSynsetid())
				&& (this.getWordid() == castOther.getWordid());
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getSynsetid() == null) ? 0 : getSynsetid().hashCode());
			result = prime * result + ((getWordid() == null) ? 0 : getWordid().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}
}
