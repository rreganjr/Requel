/*
 * $Id: LexlinkrefId.java,v 1.2 2009/01/03 10:24:33 rregan Exp $
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
import javax.persistence.Embeddable;

/**
 * @author ron
 */
@Embeddable
public class LexlinkrefId implements Comparable<LexlinkrefId>, Serializable {
	static final long serialVersionUID = 0;

	private Long word1id;
	private Long synset1id;
	private Long word2id;
	private Long synset2id;
	private Long linkid;

	public LexlinkrefId() {
	}

	public LexlinkrefId(Long word1id, Long synset1id, Long word2id, Long synset2id, Long linkid) {
		this.word1id = word1id;
		this.synset1id = synset1id;
		this.word2id = word2id;
		this.synset2id = synset2id;
		this.linkid = linkid;
	}

	@Column(name = "word1id", nullable = false)
	public Long getWord1id() {
		return this.word1id;
	}

	public void setWord1id(Long word1id) {
		this.word1id = word1id;
	}

	@Column(name = "synset1id", nullable = false)
	public Long getSynset1id() {
		return this.synset1id;
	}

	public void setSynset1id(Long synset1id) {
		this.synset1id = synset1id;
	}

	@Column(name = "word2id", nullable = false)
	public Long getWord2id() {
		return this.word2id;
	}

	public void setWord2id(Long word2id) {
		this.word2id = word2id;
	}

	@Column(name = "synset2id", nullable = false)
	public Long getSynset2id() {
		return this.synset2id;
	}

	public void setSynset2id(Long synset2id) {
		this.synset2id = synset2id;
	}

	@Column(name = "linkid", nullable = false)
	public Long getLinkid() {
		return this.linkid;
	}

	public void setLinkid(Long linkid) {
		this.linkid = linkid;
	}

	@Override
	public boolean equals(Object other) {
		if ((this == other)) {
			return true;
		}
		if ((other == null)) {
			return false;
		}
		if (!(other instanceof LexlinkrefId)) {
			return false;
		}
		LexlinkrefId castOther = (LexlinkrefId) other;

		return (this.getWord1id() == castOther.getWord1id())
				&& (this.getSynset1id() == castOther.getSynset1id())
				&& (this.getWord2id() == castOther.getWord2id())
				&& (this.getSynset2id() == castOther.getSynset2id())
				&& (this.getLinkid() == castOther.getLinkid());
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			final int prime = 31;
			int result = 1;
			result = prime * result + getSynset1id().hashCode();
			result = prime * result + getWord1id().hashCode();
			result = prime * result + getSynset2id().hashCode();
			result = prime * result + getWord2id().hashCode();
			result = prime * result + getLinkid().hashCode();
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public int compareTo(LexlinkrefId o) {
		if (this.getSynset1id() != o.getSynset1id()) {
			return (getSynset1id().intValue() - o.getSynset1id().intValue());
		}
		if (this.getSynset2id() != o.getSynset2id()) {
			return (this.getSynset2id().intValue() - o.getSynset2id().intValue());
		}
		if (this.getLinkid() != o.getLinkid()) {
			return (this.getLinkid().intValue() - o.getLinkid().intValue());
		}
		return 0;
	}
}
